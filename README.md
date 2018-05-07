#miaosha

##项目目标
支撑“小米印度抢购搞挂亚马逊事件”：http://bbs.xiaomi.cn/t-13417592
> 小米在印度打破了多项记录：
1. 4分钟内卖出了超过250,000台。 ---OPS:1042次抢购/S
2. 成为最快的手机抢购活动。
3. 抢购前我们收到了100万“到货提醒”。
4. 亚马逊每分钟收到超过500万个点击。
5. 亚马逊在这个期间每秒收到1500个订单（这是印度电商公司所有销售中最高的）。  ---OPS：1500次下单请求/S

##涉及到的框架说明
1. MVC框架：http://git.oschina.net/1028125449/SMVC
2. redis消息队列：http://git.oschina.net/1028125449/message-trunk

##实现结构
恶意请求过滤-->限流-->redis消息队列执行占位操作，获得下单token-->用户传入token下单
如下为抢购流程：
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/004233_4a6509ef_50648.png "在这里输入图片标题")
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/004247_f310a750_50648.png "在这里输入图片标题")
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/004256_770ffe62_50648.png "在这里输入图片标题")
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/004305_c8d7558e_50648.png "在这里输入图片标题")

##请求过滤
1. 入口只有活动开启前才能获得
2. 入口恶意用户检测：多秒内多少次请求---可以记录最近10次请求时间，和前第九次请求时间对比

##实时限流器
1. 实时限流：限制正在处理的请求量（通过消息队列获取正在处理的请求数目）为库存的100倍请求（这个可自定义）；
2. 如果出现了限流器满了，但仍然有库存的情况怎么办？直接拒绝请求，允许用户重新提交请求
##请求减库存
1. 请求通过了过滤之后，交给消息队列减库存+下单
##消息队列处理
1. 消息队列再次过滤请求是否是恶意的用户
2. 否则，执行减库存+下单

##各个方案
### 1. 直接更新数据库：
磁盘IO，开发机器实测2280 OPS，速度太低，当出现海量请求时会导致大量请求线程被阻塞，拒绝后续请求，拖垮整个tomcat和DB。

###2. redis+消息队列+更新数据库（秒杀和下单操作分离）
* a.用户请求过来，将请求入消息队列；
* b.消息处理，先减redis库存量，如果减库存成功，则生成下单token存入redis（设定有效期，比如2分钟之内下单有效），等待用户下单（这样就避免下单也面对大量并发）；如果减库存失败，则消息记录回到消息队列中，等待再次处理；
* c.用户下单：判断token是否失效（比对时间）了，如果未失效则扣减库存（也可能扣减库存失败），生成订单；如果已经失效了，则redis库存增加1；
如何确保下单token过期了释放资格？JOB 每分钟扫token缓存，如果失效了的则清除调，并回馈redis缓存（redis库存+1）；

* d、前端用户如何获知抢购成功了（获得了下单资格）：ajax轮训查询接口。
说明：为什么要采用轮询而不是用实时的websocket推送？经测试，一台tomcat最多能连接3000个websocket，如果类似抢购的大量用户抢购，机器肯定是扛不住这么多长连接的，而查询用户是否抢购成功也只是查询的redis，因此采用轮询是很好的选择。
* e、为什么要秒杀和下单操作分离？一方面，秒杀接口可以阻挡大部分并发流程，从而让下单操作错开并发高峰；另一方面，可以让秒杀操作和下单操作从业务上相分离，使得秒杀操作可以独立于订单相关业务。

###3. 防刷过滤器+redis+消息队列+更新数据库
针对第2方案中可能出现被辅助软件而已刷单的现象，可以增加过滤器：如果用户在指定时间内请求多少次，则认为是恶意用户，可以直接将该用户加入黑名单，并在后续的消息队列处理中不给黑名单的用户分配资格。

![输入图片说明](https://git.oschina.net/uploads/images/2017/0503/005452_8dfd9605_50648.png "在这里输入图片标题")
![输入图片说明](https://git.oschina.net/uploads/images/2017/0503/005942_31910b14_50648.png "在这里输入图片标题")
消息队列异步处理流程图：![消息队列异步处理流程图](https://git.oschina.net/uploads/images/2017/0503/010018_ed8105e3_50648.png "消息队列异步处理流程图")

##性能测试
* 测试环境说明：受限于机器资源有限，以下测试的tomcat、redis、jmeter(version：3.1)均运行于一台“4核、16GB、256SSD”的机器。因此测试结果会偏低很多，相信部到生产环境会有更好的结果。
* 测试步骤：jmeter持续发送抢购请求，tomcat收到请求，向消息队列推入消息，消息队列收到消息同时开始处理。

### 25W库存 30W次抢购请求
* 抢购接口响应：
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/002214_a0adb099_50648.png "在这里输入图片标题")
测试结果：300000次请求，QPS:1592.077
* 抢购结果：5:14秒消化完所有抢购消息，处理速度：796个商品/S

### 25W库存 100W次抢购请求
* 抢购接口响应：
![输入图片说明](https://git.oschina.net/uploads/images/2017/0504/000126_cb65d4f3_50648.png "在这里输入图片标题")
测试结果：1066155次请求，QPS:1035.749
* 抢购结果：13:30秒消化完所有抢购消息，处理速度：308个商品/S（为什么该数据这么低，因为抢购接口持续被请求，占用了大量CPU资源，导致处理消息队列线程池线程无法快速消化消息）


##前端页面
前端技术拙劣，所有前端页面均来自于GitHub上（实在找不到原地址是什么了。。）

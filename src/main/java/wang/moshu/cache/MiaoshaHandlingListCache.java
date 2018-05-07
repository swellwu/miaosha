package wang.moshu.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import wang.moshu.constant.CommonConstant;
import wang.moshu.util.RedisUtil;

/**
 * 秒杀正在处理请求列表
 *
 * @author xiangyong.ding@weimob.com
 * @category 秒杀正在处理请求列表
 * @since 2017年4月20日 上午12:01:16
 */
@Component
public class MiaoshaHandlingListCache {
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 增加到处理列表
     *
     * @param mobile
     * @param goodsRandomName
     * @category @author xiangyong.ding@weimob.com
     * @since 2017年4月19日 下午11:59:35
     */
    public void add2HanleList(String mobile, String goodsRandomName) {
        redisUtil.hset(getKey(goodsRandomName), mobile, mobile);
    }

    /**
     * 增加到处理列表
     *
     * @param mobile
     * @param goodsRandomName
     * @category @author xiangyong.ding@weimob.com
     * @since 2017年4月19日 下午11:59:35
     */
    public void removeFromHanleList(String mobile, String goodsRandomName) {
        redisUtil.hdel(getKey(goodsRandomName), mobile);
    }

    /**
     * 是否在正在处理列表中
     *
     * @param mobile
     * @param goodsRandomName
     * @category 是否在正在处理列表中
     * @author xiangyong.ding@weimob.com
     * @since 2017年4月19日 下午11:59:35
     */
    public boolean isInHanleList(String mobile, String goodsRandomName) {
        return redisUtil.hget(getKey(goodsRandomName), mobile, String.class) != null;
    }

    private String getKey(String goodsRandomName) {
        return CommonConstant.RedisKey.MIAOSHA_HANDLE_LIST + goodsRandomName;
    }

}

package wang.moshu.cache;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import wang.moshu.cache.base.CurrentLimiter;
import wang.moshu.constant.CommonConstant;
import wang.moshu.constant.MessageType;
import wang.moshu.dao.GoodsMapper;
import wang.moshu.message.MessageMonitor;

/**
 * 商品购买限流器
 *
 * @author xiangyong.ding@weimob.com
 * @category 商品购买限流器
 * @since 2017年3月16日 下午1:54:05
 */
@Component
public class GoodsBuyCurrentLimiter extends CurrentLimiter<String> {
    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private MessageMonitor messageMonitor;

    @Override
    protected String getLimiterName(String goodsRandomName) {
        String key = MessageFormat.format(CommonConstant.RedisKey.GOODS_STORE_BY_ID, new Object[]{goodsRandomName});
        return key;
    }

    @Override
    protected int getLimit(String goodsRandomName) {
        return goodsMapper.selectByRandomName(goodsRandomName).getStore()
                * CommonConstant.CurrentLimitMultiple.GOODS_BUY;
    }

    @Override
    protected int getCurrentLimit() {
        return messageMonitor.getMessageLeft(MessageType.MIAOSHA_MESSAGE);
    }

}

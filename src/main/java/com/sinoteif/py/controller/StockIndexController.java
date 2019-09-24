package com.sinoteif.py.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sinoteif.py.mapper.StockIndexMapper;
import com.sinoteif.py.utils.PinyinUtils;
import com.sinoteif.py.vo.StockIndex;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2019/9/23.
 */
@RestController
public class StockIndexController {
    public static final Map<String,String> STOCK_EXCHANGE;
    private static Logger logger= LoggerFactory.getLogger(StockIndexController.class);
    private static final Pattern NUM_PATTERN=Pattern.compile("[0-9]{0,6}");
    private static final Pattern CHAR_PATTERN=Pattern.compile("[a-z]+|[A-Z]+");

    static {
        STOCK_EXCHANGE = new HashMap<String,String>();
        STOCK_EXCHANGE.put("600", "sh");
        STOCK_EXCHANGE.put("601", "sh");
        STOCK_EXCHANGE.put("603", "sh");

        STOCK_EXCHANGE.put("000", "sz");
        STOCK_EXCHANGE.put("001", "sz");
        STOCK_EXCHANGE.put("002", "sz");
        STOCK_EXCHANGE.put("300", "sz");
    }
    @Resource
    private RedisTemplate redisTemplate;
    @Autowired
    private StockIndexMapper stockIndexMapper;

    @GetMapping("/stockIndex/{stockPreLetter}")
    public List<Map<String,String>> l2(@PathVariable("stockPreLetter") String stockPreLetter){
        logger.info("stockPreLetter=" + stockPreLetter);
        Matcher numMatcher=NUM_PATTERN.matcher(stockPreLetter);
        Matcher charMatcher=CHAR_PATTERN.matcher(stockPreLetter);

        if(!numMatcher.find() && !charMatcher.find()){
            return new ArrayList<Map<String,String>>();
        }
        List<Map<String,String>> list=null;
        if(numMatcher.find()){
            list= stockIndexMapper.selectStockByStockCode(stockPreLetter);
        }
        if(charMatcher.find()){
            list= stockIndexMapper.selectStockByPrefix(stockPreLetter);
        }
        logger.debug("stockList",JSON.toJSONString(list));
        return list;
    }
    @GetMapping("/stockIndex/initAll")
    public Object initAll() throws BadHanyuPinyinOutputFormatCombination {
        Set<String> set=redisTemplate.keys("*");
        logger.info("~~~~~~~~~~~~~~");
        int i=0;
        for(String stockCode:set){
            if(null != stockCode && stockCode.length() == 6) {
                String stockType = STOCK_EXCHANGE.get(stockCode.substring(0, 3));
                if(null == stockType) {
                    continue;
                }
            }
            Object stockInfo= redisTemplate.opsForValue().get(stockCode);
            if (stockInfo!=null){
                JSONObject jsonObject=  JSON.parseObject(stockInfo.toString());
                String stock=jsonObject.getString("code");
                String stockName=jsonObject.getString("chName");
                /**
                 * 多音字版 4551条记录
                 */
//                String tmp= Pinyin4JUtils.converterToFirstSpell(stockName).toUpperCase();
//                String[] names=tmp.split(",");
//                for(String name:names){
//                    StockIndex stockIndex=new StockIndex();
//                    stockIndex.setStockCode(stock);
//                    stockIndex.setStockName(stockName);
//                    stockIndex.setStockNameFirstLetter(name);
//                    stockIndexMapper.insertSelective(stockIndex);
//                    i++;
//                }
                /**
                 * 单音字
                 */
                String stockNameFL=PinyinUtils.getAlpha(stockName);
                StockIndex stockIndex=new StockIndex();
                stockIndex.setStockCode(stock);
                stockIndex.setStockName(stockName);
                stockIndex.setStockNameFirstLetter(stockNameFL);
                stockIndexMapper.insertSelective(stockIndex);
            }
        }
        return i;
    }
}
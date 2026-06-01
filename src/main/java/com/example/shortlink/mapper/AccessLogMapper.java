package com.example.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shortlink.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    @Select("SELECT COUNT(*) as count, DATE(access_time) as date FROM access_log WHERE short_code = #{shortCode} GROUP BY DATE(access_time) ORDER BY date DESC LIMIT 7")
    Map<String, Object> getAccessStatistics(@Param("shortCode") String shortCode);

    @Select("SELECT COUNT(*) FROM access_log WHERE short_code = #{shortCode}")
    Long countAccessByShortCode(@Param("shortCode") String shortCode);
}
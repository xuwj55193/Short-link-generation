package com.example.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shortlink.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {

    @Select("SELECT * FROM short_link WHERE short_code = #{shortCode}")
    ShortLink selectByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT * FROM short_link WHERE long_url = #{longUrl}")
    ShortLink selectByLongUrl(@Param("longUrl") String longUrl);
}
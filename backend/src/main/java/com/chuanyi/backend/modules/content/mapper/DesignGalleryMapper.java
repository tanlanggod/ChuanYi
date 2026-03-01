package com.chuanyi.backend.modules.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanyi.backend.modules.content.entity.DesignGalleryEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DesignGalleryMapper extends BaseMapper<DesignGalleryEntity> {

    @Select("SELECT COUNT(1) FROM design_gallery WHERE display_status = 'ENABLED'")
    long countEnabled();

    @Select("SELECT g.* " +
            "FROM design_gallery g " +
            "LEFT JOIN design_gallery_stat s ON s.gallery_id = g.id " +
            "WHERE g.display_status = 'ENABLED' " +
            "ORDER BY (IFNULL(s.like_count, 0) * 10 + IFNULL(s.view_count, 0)) DESC, g.published_at DESC, g.id DESC " +
            "LIMIT #{offset}, #{limit}")
    List<DesignGalleryEntity> selectHotPage(@Param("offset") long offset, @Param("limit") long limit);
}

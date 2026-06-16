package com.aquarium.service.mapper;

import com.aquarium.service.entity.WaterQualityRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WaterQualityRecordMapper extends BaseMapper<WaterQualityRecord> {

    @Select("SELECT * FROM water_quality_record WHERE tank_id = #{tankId} " +
            "AND recorded_at BETWEEN #{startTime} AND #{endTime} ORDER BY recorded_at DESC")
    List<WaterQualityRecord> findByTankIdAndTimeRange(@Param("tankId") String tankId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM water_quality_record WHERE tank_id = #{tankId} " +
            "ORDER BY recorded_at DESC LIMIT #{limit}")
    List<WaterQualityRecord> findLatestByTankId(@Param("tankId") String tankId,
                                                 @Param("limit") int limit);
}

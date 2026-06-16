package com.aquarium.service.mapper;

import com.aquarium.service.entity.AlertRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {

    @Select("SELECT * FROM alert_record WHERE tank_id = #{tankId} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<AlertRecord> findByTankIdAndTimeRange(@Param("tankId") String tankId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM alert_record WHERE level = #{level} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<AlertRecord> findByLevel(@Param("level") String level,
                                   @Param("limit") int limit);

    @Select("SELECT * FROM alert_record WHERE tank_id = #{tankId} " +
            "AND level IN ('critical', 'fatal') ORDER BY created_at DESC LIMIT #{limit}")
    List<AlertRecord> findCriticalByTankId(@Param("tankId") String tankId,
                                            @Param("limit") int limit);
}

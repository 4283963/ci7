package com.aquarium.service.mapper;

import com.aquarium.service.entity.Device;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Select("SELECT * FROM device WHERE tank_id = #{tankId}")
    List<Device> findByTankId(@Param("tankId") String tankId);

    @Select("SELECT * FROM device WHERE device_id = #{deviceId}")
    Device findByDeviceId(@Param("deviceId") String deviceId);

    @Update("UPDATE device SET status = #{status}, last_heartbeat = #{heartbeat}, " +
            "updated_at = NOW() WHERE device_id = #{deviceId}")
    int updateStatus(@Param("deviceId") String deviceId,
                     @Param("status") String status,
                     @Param("heartbeat") LocalDateTime heartbeat);
}

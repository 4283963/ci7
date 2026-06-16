package com.aquarium.service.mapper;

import com.aquarium.service.entity.BreedingConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BreedingConfigMapper extends BaseMapper<BreedingConfig> {

    @Select("SELECT * FROM breeding_config WHERE tank_id = #{tankId}")
    BreedingConfig findByTankId(@Param("tankId") String tankId);

    @Select("SELECT * FROM breeding_config WHERE mode_status != 'idle'")
    List<BreedingConfig> findActiveBreeding();

    @Select("SELECT * FROM breeding_config WHERE species = #{species}")
    List<BreedingConfig> findBySpecies(@Param("species") String species);
}

package com.careld.store.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.store.entity.Store;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
    @Select("SELECT * FROM store_info WHERE store_code = #{storeCode} AND deleted_at IS NULL")
    Store selectByStoreCode(String storeCode);
}

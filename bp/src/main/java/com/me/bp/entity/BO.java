package com.me.bp.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @ClassName BO
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/24 17:36
 * @Version 1.0
 */
@Data
@Table("bo")
public class BO {
    @Id(keyType = KeyType.Auto)
    private int id;
    private String name;
    private String type;
    private String config;
    private String createTime;
    private String updateTime;
}

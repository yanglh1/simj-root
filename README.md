# simJ Root

simJ Magisk 特权模块 — 绕过 ARA-M，直接访问内置 eSIM。

## 原理

将 simJ 安装为 `/system/priv-app/` 系统特权应用，自动获得 `SECURE_ELEMENT` 权限，无需 ARA-M 签名白名单。

## 前置条件

- Bootloader 已解锁
- 已安装 Magisk（v20.4+）

## 安装

1. 下载 [Release](https://github.com/yanglh1/simj-root/releases) 中的 zip 文件
2. 打开 Magisk → 模块 → 从本地安装
3. 选择下载的 zip
4. 重启手机
5. 打开 simJ，进入 eSIM 页面

## 模块结构

```
simj-privileged-v3.0.5.zip
├── META-INF/com/google/android/
│   ├── update-binary
│   └── updater-script
├── module.prop
├── customize.sh
└── system/
    ├── priv-app/com.sansim.app/
    │   └── com.sansim.app.apk
    └── etc/permissions/
        └── privapp-permissions-com.sansim.app.xml
```

## 授予的权限

| 权限 | 用途 |
|------|------|
| `SECURE_ELEMENT` | 访问 eSIM (eSE) |
| `READ_PRIVILEGED_PHONE_STATE` | 读取设备状态 |
| `MODIFY_PHONE_STATE` | 管理 SIM/eSIM |
| `ACCESS_FINE_LOCATION` | 运营商信息 |
| `ACCESS_BACKGROUND_LOCATION` | 后台同步 |

## 卸载

在 Magisk 中禁用或删除模块，重启即可恢复普通安装状态。

## 注意

- 仅适用于 Root 设备
- 普通用户请使用标准 APK（需要实体卡读卡器）
- 系统更新后可能需要重新安装模块

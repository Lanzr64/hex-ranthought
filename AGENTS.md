# 仓库贡献指南

## 项目结构

本仓库是适用于 Forge 1.20.1 的 Hex Ranthought 模组。Java 代码位于 `src/main/java/com/lanzr/hexranthought/`：`casting/` 实现图案与法术，`blocks/` 和 `items/` 定义方块与物品，`client/` 负责客户端效果，`network/` 处理网络消息。资源位于 `src/main/resources/`：`assets/hex_ranthought/` 包含纹理、模型和翻译；`assets/hexcasting/patchouli_books/` 包含 Hex Casting 手册条目；`data/hex_ranthought/` 包含配方和战利品表。`docs/` 存放补充说明。同级的 `Hexal/` 和 `HexGloop/` 是参考项目，不属于本工程的构建模块。

## 构建、测试与本地运行

使用 Java 17 和仓库自带的 Gradle Wrapper。本机查找或安装开发环境，以及存放缓存时，统一使用 `D:\ENV`：

```powershell
$env:JAVA_HOME='D:\ENV\jdk\jdk-17.0.20.1+1'
$env:GRADLE_USER_HOME='D:\ENV\gradle-cache'
.\gradlew.bat build
```

`build` 会编译、运行已配置的测试，并在 `build/libs/` 生成模组 JAR。使用 `.\gradlew.bat runClient` 在游戏中检查功能；修改生成资源时使用 `.\gradlew.bat runData`。`gradle.properties` 定义模组版本和 Hex Casting 依赖范围；修改兼容范围前须验证相应版本。

## 代码风格与命名

Java 使用四个空格缩进，JSON 使用两个空格缩进。沿用现有包结构；类名采用清晰的 PascalCase，例如 `ThaumicShockAction`。注册 ID 和资源文件名使用 `hex_ranthought` 命名空间及小写下划线格式。修改玩家可见内容时，同步更新 `en_us.json`、`zh_cn.json`；新增图案或物品时，也更新对应的手册条目。仓库没有配置统一的格式化器或检查器，请遵循相邻代码的风格。

## 测试要求

目前没有项目测试套件或覆盖率指标。修改代码或资源后运行 `build`；修改法术、方块或渲染时，条件允许还应使用 `runClient` 实际验证。新增 Java 测试放在 `src/test/java/`，文件名以 `Test.java` 结尾，并通过 `.\gradlew.bat test` 运行。

## 提交与拉取请求

Git 历史目前只有初始化提交，尚无既定提交信息规范。建议使用简短、祈使语气的主题说明具体改动。拉取请求应说明行为变化、列出验证命令及结果，并关联相关议题；纹理、特效或手册界面有变化时附上截图。不要提交生成的 `build/` 文件。

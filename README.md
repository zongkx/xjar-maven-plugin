## 说明

本插件提供了 springboot3 的 xjar 加固能力, 目前未包含 jdk md5 校验功能




```xml
            <plugin>
                <groupId>com.zongkx</groupId>
                <artifactId>xjar-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <phase>package</phase> <goals>
                        <goal>build</goal>
                    </goals>
                    </execution>
                </executions>
                <configuration>
                    <password>YourSecurePassword123!</password>
                    <!--可选* (空则使用环境变量)-->
                    <goPath>C:\dev\go\bin\go.exe</goPath>
                    <includes>
                        <include>com/example/**/*.class</include>
                    </includes>
                </configuration>
            </plugin>
```

## 特别鸣谢
`xjar`的 springboot3 兼容由该仓库提供
[source](https://github.com/MisterChangRay/xjar4springboot3)

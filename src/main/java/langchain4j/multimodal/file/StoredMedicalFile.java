// 声明多模态医疗文件公共模型所在的包。
package langchain4j.multimodal.file;

// 引入文件系统路径类型，用于定位私有存储中的文件。
import java.nio.file.Path;

/** 存储层内部文件对象，磁盘路径path不会通过 Controller 返回。 */
//这是一个Java Record（不可变数据载体），只存在于JVM内存中，不是最终存在数据库里的结构
//StoredMedicalFile 是一个临时组合对象，作用是将"可以安全暴露给外部的元数据"和"仅内部使用的磁盘路径"打包在一起，
// 方便存储层和处理层之间传递。它从始至终都只在内存中流转，不进入任何数据库。
public record StoredMedicalFile(
        // 保存可安全返回给的文件前端（业务层）的元数据。
        UploadedMedicalFile metadata,
        // 保存仅限后端（存储层和处理层）使用的本地绝对路径。
        // path字段的绝路径是在应用运行时通过配置动态生成的，并非硬编码在代码中。因此不会因为代码转移到其他电脑而不兼容
        Path path
) {
}

package langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import langchain4j.entity.DoctorInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 医生信息 Mapper 接口
 * 
 * @author yangyue
 * @date 2026/4/21
 */
@Mapper
public interface DoctorInfoMapper extends BaseMapper<DoctorInfo> {
    
    /**
     * 根据科室查询在职医生列表（按排序字段升序）
     * 
     * @param department 科室名称
     * @return 医生列表
     */
    @Select("SELECT * FROM doctor_info WHERE department = #{department} AND status = 1 ORDER BY sort_order ASC")
    List<DoctorInfo> findByDepartment(@Param("department") String department);
    
    /**
     * 根据科室查询在职医生列表（包含职称排序）
     * 
     * @param department 科室名称
     * @return 医生列表（按职称优先级排序：主任医师 > 副主任医师 > 主治医师 > 住院医师）
     */
    @Select("SELECT * FROM doctor_info WHERE department = #{department} AND status = 1 " +
            "ORDER BY FIELD(title, '主任医师', '副主任医师', '主治医师', '住院医师'), sort_order ASC")
    List<DoctorInfo> findByDepartmentWithTitleOrder(@Param("department") String department);
    
    /**
     * 查询所有在职医生（按科室和排序）
     * 
     * @return 所有在职医生列表
     */
    @Select("SELECT * FROM doctor_info WHERE status = 1 ORDER BY department, sort_order ASC")
    List<DoctorInfo> findAllActiveDoctors();
    
    /**
     * 根据医生姓名查询
     * 
     * @param doctorName 医生姓名
     * @return 医生信息
     */
    @Select("SELECT * FROM doctor_info WHERE doctor_name = #{doctorName} AND status = 1")
    DoctorInfo findByDoctorName(@Param("doctorName") String doctorName);
    
    /**
     * 根据科室和职称查询医生
     * 
     * @param department 科室名称
     * @param title 职称
     * @return 医生列表
     */
    @Select("SELECT * FROM doctor_info WHERE department = #{department} AND title = #{title} AND status = 1")
    List<DoctorInfo> findByDepartmentAndTitle(@Param("department") String department, @Param("title") String title);
    
    /**
     * 搜索医生（按姓名或科室模糊匹配）
     * 
     * @param keyword 搜索关键词
     * @return 医生列表
     */
    @Select("SELECT * FROM doctor_info WHERE status = 1 AND (doctor_name LIKE CONCAT('%', #{keyword}, '%') OR department LIKE CONCAT('%', #{keyword}, '%'))")
    List<DoctorInfo> searchDoctors(@Param("keyword") String keyword);
    
    /**
     * 获取科室列表（所有有在职医生的科室）
     * 
     * @return 科室名称列表
     */
    @Select("SELECT DISTINCT department FROM doctor_info WHERE status = 1 ORDER BY department")
    List<String> getAllDepartments();
    
    /**
     * 获取科室的医生数量
     * 
     * @param department 科室名称
     * @return 医生数量
     */
    @Select("SELECT COUNT(*) FROM doctor_info WHERE department = #{department} AND status = 1")
    int countDoctorsByDepartment(@Param("department") String department);
    
    /**
     * 更新医生状态（离职/在职）
     * 
     * @param doctorName 医生姓名
     * @param status 状态（0-离职，1-在职）
     * @return 更新行数
     */
    @Update("UPDATE doctor_info SET status = #{status}, update_time = NOW() WHERE doctor_name = #{doctorName}")
    int updateDoctorStatus(@Param("doctorName") String doctorName, @Param("status") Integer status);
}
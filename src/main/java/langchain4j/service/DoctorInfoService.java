package langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import langchain4j.entity.DoctorInfo;

import java.util.List;

/**
 * 医生信息服务接口
 * 
 * @author yangyue
 * @date 2026/4/21
 */
public interface DoctorInfoService extends IService<DoctorInfo> {
    
    /**
     * 根据科室查询在职医生列表
     * 
     * @param department 科室名称
     * @return 医生列表
     */
    List<DoctorInfo> getDoctorsByDepartment(String department);
    
    /**
     * 根据科室查询在职医生列表（按职称排序）
     * 
     * @param department 科室名称
     * @return 医生列表
     */
    List<DoctorInfo> getDoctorsByDepartmentWithTitleOrder(String department);
    
    /**
     * 查询所有在职医生
     * 
     * @return 所有在职医生列表
     */
    List<DoctorInfo> getAllActiveDoctors();
    
    /**
     * 根据医生姓名查询
     * 
     * @param doctorName 医生姓名
     * @return 医生信息，不存在返回null
     */
    DoctorInfo getDoctorByName(String doctorName);
    
    /**
     * 根据科室和职称查询医生
     * 
     * @param department 科室名称
     * @param title 职称
     * @return 医生列表
     */
    List<DoctorInfo> getDoctorsByDepartmentAndTitle(String department, String title);
    
    /**
     * 搜索医生
     * 
     * @param keyword 搜索关键词
     * @return 医生列表
     */
    List<DoctorInfo> searchDoctors(String keyword);
    
    /**
     * 获取所有科室列表
     * 
     * @return 科室名称列表
     */
    List<String> getAllDepartments();
    
    /**
     * 格式化医生信息用于展示
     * 
     * @param doctorInfo 医生信息
     * @return 格式化后的字符串
     */
    String formatDoctorInfo(DoctorInfo doctorInfo);
    
    /**
     * 格式化医生简介用于大模型
     * 
     * @param doctorInfo 医生信息
     * @return 格式化后的简介
     */
    String formatDoctorIntroduction(DoctorInfo doctorInfo);
}
package langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import langchain4j.entity.DoctorInfo;
import langchain4j.mapper.DoctorInfoMapper;
import langchain4j.service.DoctorInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生信息服务实现类
 * 
 * @author yangyue
 * @date 2026/4/21
 */
@Service
public class DoctorInfoServiceImpl extends ServiceImpl<DoctorInfoMapper, DoctorInfo> 
        implements DoctorInfoService {
    
    @Override
    public List<DoctorInfo> getDoctorsByDepartment(String department) {
        return baseMapper.findByDepartment(department);
    }
    
    @Override
    public List<DoctorInfo> getDoctorsByDepartmentWithTitleOrder(String department) {
        return baseMapper.findByDepartmentWithTitleOrder(department);
    }
    
    @Override
    public List<DoctorInfo> getAllActiveDoctors() {
        return baseMapper.findAllActiveDoctors();
    }
    
    @Override
    public DoctorInfo getDoctorByName(String doctorName) {
        return baseMapper.findByDoctorName(doctorName);
    }
    
    @Override
    public List<DoctorInfo> getDoctorsByDepartmentAndTitle(String department, String title) {
        return baseMapper.findByDepartmentAndTitle(department, title);
    }
    
    @Override
    public List<DoctorInfo> searchDoctors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveDoctors();
        }
        return baseMapper.searchDoctors(keyword);
    }
    
    @Override
    public List<String> getAllDepartments() {
        return baseMapper.getAllDepartments();
    }
    
    @Override
    public String formatDoctorInfo(DoctorInfo doctor) {
        if (doctor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(doctor.getDoctorName()).append("】");
        sb.append("（").append(doctor.getTitle()).append("）\n");
        if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
            sb.append("  擅长：").append(doctor.getSpecialty()).append("\n");
        }
        if (doctor.getIntroduction() != null && !doctor.getIntroduction().isEmpty()) {
            // 只显示前100字
            String intro = doctor.getIntroduction().length() > 100 
                ? doctor.getIntroduction().substring(0, 100) + "..." 
                : doctor.getIntroduction();
            sb.append("  简介：").append(intro);
        }
        return sb.toString();
    }
    
    @Override
    public String formatDoctorIntroduction(DoctorInfo doctor) {
        if (doctor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(doctor.getDoctorName()).append("，").append(doctor.getTitle());
        if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
            sb.append("，擅长").append(doctor.getSpecialty());
        }
        return sb.toString();
    }
}
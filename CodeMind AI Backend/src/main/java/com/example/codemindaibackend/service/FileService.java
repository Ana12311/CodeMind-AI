package com.example.codemindaibackend.service;

import com.example.codemindaibackend.common.result.PageResult;
import com.example.codemindaibackend.dto.file.FileQueryRequest;
import com.example.codemindaibackend.vo.file.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 代码文件业务接口
 *
 * @author CodeMind
 */
public interface FileService {

    /**
     * 分页查询文件
     */
    PageResult<FileVO> pageFiles(FileQueryRequest request);

    /**
     * 文件详情
     */
    FileVO getFile(Long id);

    /**
     * 读取文件内容（小文本文件取 DB content，大文件回退存储加载）
     */
    String getFileContent(Long id);

    /**
     * 上传文件并保存信息
     */
    FileVO upload(MultipartFile file, Long projectId);

    /**
     * 删除文件（逻辑删除）
     */
    void deleteFile(Long id);
}

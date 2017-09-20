package com.yupaits.docs.rest;

import com.yupaits.docs.common.response.Result;
import com.yupaits.docs.common.response.ResultCode;
import com.yupaits.docs.config.jwt.JwtHelper;
import com.yupaits.docs.entity.Document;
import com.yupaits.docs.entity.DocumentHistory;
import com.yupaits.docs.repository.DocumentHistoryRepository;
import com.yupaits.docs.repository.DocumentRepository;
import com.yupaits.docs.util.http.HttpUtil;
import com.yupaits.docs.util.validate.ValidateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;

/**
 * 文档REST接口
 * Created by yupaits on 2017/8/5.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentHistoryRepository documentHistoryRepository;

    @Autowired
    private JwtHelper jwtHelper;

    @GetMapping("/{documentId}")
    public Result getDocumentById(@PathVariable Integer documentId) {
        if (ValidateUtils.idInvalid(documentId)) {
            return Result.fail(ResultCode.PARAMS_ERROR);
        }
        Document document = documentRepository.findOne(documentId);
        if (document != null && document.getOwnerId().compareTo(jwtHelper.getUserId(HttpUtil.getRequest())) != 0) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        return Result.ok(document);
    }

    @PostMapping("")
    public Result createDocument(@RequestBody Document document) {
        if (document == null || ValidateUtils.idInvalid(document.getOwnerId())
                || ValidateUtils.idInvalid(document.getDirectoryId()) || StringUtils.isBlank(document.getName())) {
            return Result.fail(ResultCode.PARAMS_ERROR);
        }
        if (document.getOwnerId().compareTo(jwtHelper.getUserId(HttpUtil.getRequest())) != 0) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        documentRepository.save(document);
        return Result.ok();
    }

    @DeleteMapping("/{documentId}")
    public Result deleteDocument(@PathVariable Integer documentId) {
        if (ValidateUtils.idInvalid(documentId)) {
            return Result.fail(ResultCode.PARAMS_ERROR);
        }
        Document documentInDb = documentRepository.findOne(documentId);
        if (documentInDb == null) {
            return Result.fail(ResultCode.DATA_NOT_FOUND);
        }
        if (documentInDb.getOwnerId().compareTo(jwtHelper.getUserId(HttpUtil.getRequest())) != 0) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        saveDocumentHistory(documentInDb);
        documentRepository.delete(documentId);
        return Result.ok();
    }

    @PutMapping("/{documentId}")
    public Result updateDocument(@RequestBody Document document) {
        if (document == null || ValidateUtils.idInvalid(document.getId()) || ValidateUtils.idInvalid(document.getDirectoryId())
                || StringUtils.isBlank(document.getName())) {
            return Result.fail(ResultCode.PARAMS_ERROR);
        }
        Document documentInDb = documentRepository.findOne(document.getId());
        if (documentInDb == null) {
            return Result.fail(ResultCode.DATA_NOT_FOUND);
        }
        if (documentInDb.getOwnerId().compareTo(jwtHelper.getUserId(HttpUtil.getRequest())) != 0) {
            return Result.fail(ResultCode.FORBIDDEN);
        }
        saveDocumentHistory(documentInDb);
        documentRepository.save(document);
        return Result.ok();
    }

    /**
     * 保存文档历史记录
     * @param document 文档
     */
    private void saveDocumentHistory(Document document) {
        DocumentHistory documentHistory = new DocumentHistory();
        if (StringUtils.isNotBlank(document.getContent())) {
            documentHistory.setOwnerId(document.getOwnerId());
            documentHistory.setDocumentId(document.getId());
            documentHistory.setContent(document.getContent());
            documentHistory.setSavedTime(new Timestamp(System.currentTimeMillis()));
            documentHistoryRepository.save(documentHistory);
        }
    }
}

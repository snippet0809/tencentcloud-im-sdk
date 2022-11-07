package io.github.snippet0809;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class BaseResponse {

    @JSONField(name = "ActionStatus")
    private String actionStatus;
    @JSONField(name = "ErrorInfo")
    private String errorInfo;
    @JSONField(name = "ErrorCode")
    private Integer errorCode;
}

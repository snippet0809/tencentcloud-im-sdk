package io.github.snippet0809.tencentimsdk;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetGroupMutedAccountResponse extends BaseResponse {

    @JSONField(name = "MutedAccountList")
    private List<MutedAccount> mutedAccountList;

    @Data
    public static class MutedAccount {

        @JSONField(name = "Member_Account")
        private String memberAccount;
        @JSONField(name = "MutedUntil")
        private Long mutedUntil;
    }
}
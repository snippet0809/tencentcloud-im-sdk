package io.github.snippet0809;

import com.alibaba.fastjson.JSONObject;
import com.tencentyun.TLSSigAPIv2;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ImClient {

    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final long userSignExpireSeconds = 24 * 3600;
    private final long sdkAppid;
    private final String key;
    private final String adminUserId;
    private final String groupId;
    private final Map<String, Map<String, Object>> userSigCache = new HashMap<>();


    public ImClient(long sdkAppid, String key, String adminUserId, String groupId) {
        this.sdkAppid = sdkAppid;
        this.key = key;
        this.adminUserId = adminUserId;
        this.groupId = groupId;
    }

    /**
     * 发送系统消息
     */
    public BaseResponse sendSysMsg(String content) throws IOException, ImException {
        Map<String, String> bodyParam = new HashMap<>();
        bodyParam.put("GroupId", groupId);
        bodyParam.put("Content", content);
        return post(ImRestApi.SEND_SYS_MSG, JSONObject.toJSONString(bodyParam), BaseResponse.class);
    }

    /**
     * 禁言用户列表
     */
    public GetGroupMutedAccountResponse getMutedAccount() throws ImException, IOException {
        Map<String, String> bodyParam = new HashMap<>();
        bodyParam.put("GroupId", groupId);
        return post(ImRestApi.MUTED_USER_LIST, JSONObject.toJSONString(bodyParam), GetGroupMutedAccountResponse.class);
    }

    /**
     * 禁言用户
     */
    public BaseResponse forbidSendMsg(Set<String> userIds, long muteSeconds) throws ImException, IOException {
        Map<String, Object> bodyParam = new HashMap<>();
        bodyParam.put("GroupId", groupId);
        bodyParam.put("Members_Account", userIds);
        bodyParam.put("MuteTime", muteSeconds);
        return post(ImRestApi.FORBID_SEND_MSG, JSONObject.toJSONString(bodyParam), BaseResponse.class);
    }


    private <T extends BaseResponse> T post(String url, String body, Class<T> tClass) throws IOException, ImException {
        // query
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("sdkappid", sdkAppid);
        queryMap.put("identifier", adminUserId);
        queryMap.put("random", new Random().nextLong());
        queryMap.put("contenttype", "json");
        queryMap.put("usersig", getUserSig(adminUserId));
        StringBuilder stringBuilder = new StringBuilder();
        queryMap.keySet().forEach(key -> stringBuilder.append(key).append("=").append(queryMap.get(key)).append("&"));
        String queryString = stringBuilder.substring(0, stringBuilder.length() - 1);
        HttpPost httpPost = new HttpPost(url.contains("?") ? url + "&" + queryString : url + "?" + queryString);
        // header & body
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        // retry
        CloseableHttpResponse response = null;
        for (int i = 0; i < 3; i++) {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                break;
            }
        }
        // parse
        String res = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        T t = JSONObject.parseObject(res, tClass);
        if (t.getErrorCode() != 0) {
            throw new ImException(t.getErrorInfo());
        }
        return t;
    }

    private String getUserSig(String userId) {
        // 一、先从缓存中拿
        Map<String, Object> map = userSigCache.get(userId);
        String userSigValueKey = "userSignValue", userSigExpireTimeKey = "userSigExpireTime";
        if (map != null) {
            String userSigValue = (String) map.get(userSigValueKey);
            Date signExpireTime = (Date) map.get(userSigExpireTimeKey);
            if (signExpireTime.getTime() - System.currentTimeMillis() > 3 * 60 * 1000L) {
                return userSigValue;
            }
        }
        // 二、生成新的签名，放进缓存
        String userSigValue = new TLSSigAPIv2(sdkAppid, key).genUserSig(userId, userSignExpireSeconds);
        map = new HashMap<>();
        map.put(userSigValueKey, userSigValue);
        map.put(userSigExpireTimeKey, new Date(System.currentTimeMillis() + userSignExpireSeconds * 1000L));
        userSigCache.put(userId, map);
        return userSigValue;
    }
}

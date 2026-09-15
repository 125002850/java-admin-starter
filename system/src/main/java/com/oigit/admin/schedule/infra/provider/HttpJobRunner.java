package com.oigit.admin.schedule.infra.provider;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.enums.ScheduleErrorCode;
import com.oigit.admin.core.exception.BizException;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
/** HTTP targets must be explicitly configured by the deployer; redirects cannot escape that boundary. */
@Component
public class HttpJobRunner implements JobRunner {
 private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(5))
     .readTimeout(Duration.ofSeconds(30)).callTimeout(Duration.ofSeconds(35))
     .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
 private final Set<String> allowedOrigins;
 public HttpJobRunner(@Value("${platform.schedule.allowed-http-origins:}") String origins) {
   allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
 }
 public boolean supports(String route) { return route != null && (route.startsWith("https://") || route.startsWith("http://")); }
 public void execute(ScheduleJobEntity job) throws Exception {
   URI uri = URI.create(job.getInvokeRoute());
   String origin = uri.getScheme()+"://"+uri.getRawAuthority();
   if (uri.getUserInfo()!=null || uri.getHost()==null || !allowedOrigins.contains(origin)) throw new BizException(ScheduleErrorCode.JOB_EXECUTION_FAILED);
   Request request = new Request.Builder().url(uri.toString()).post(RequestBody.create("", MediaType.get("application/json"))).build();
   try (Response response = client.newCall(request).execute()) {
     if (!response.isSuccessful()) throw new BizException(ScheduleErrorCode.JOB_EXECUTION_FAILED);
   }
 }
}

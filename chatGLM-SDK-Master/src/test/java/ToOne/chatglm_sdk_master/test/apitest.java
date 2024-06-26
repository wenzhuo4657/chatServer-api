package ToOne.chatglm_sdk_master.test;

import ToOne.chatglm_sdk_master.model.RequestSSE;
import ToOne.chatglm_sdk_master.model.ResponseStream;
import ToOne.chatglm_sdk_master.model.ResponseSync;
import ToOne.chatglm_sdk_master.model.Role;
import ToOne.chatglm_sdk_master.session.Configuration;
import ToOne.chatglm_sdk_master.session.OpenAiSession;
import ToOne.chatglm_sdk_master.session.OpenAiSessionFactory;
import ToOne.chatglm_sdk_master.session.defaults.DefaultOpenAiSessionFactory;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * @className: apitest
 * @author: wenzhuo4657
 * @date: 2024/5/21 10:48
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class apitest {
    private OpenAiSession session;

    @Before
    public void test_OpenaAiSessionFactory(){
        Configuration configuration=new Configuration();
        configuration.setApiHost("https://open.bigmodel.cn/");
        configuration.setApiSecretKey("dd48fa165df16a61c7add47c69dcd099.y88gEvsbZcVi65ja");
        configuration.setLevel(HttpLoggingInterceptor.Level.BODY);

        OpenAiSessionFactory factory=new DefaultOpenAiSessionFactory(configuration);
        this.session=factory.openSession();
    }

      /**
         *  des: sse同步调用
         * */

      @SneakyThrows
      @Test
      public void test_SSE_sync() throws JsonProcessingException, InterruptedException {
          CountDownLatch countDownLatch = new CountDownLatch(1);
          RequestSSE request=new RequestSSE();
          request.setStream(false);
          request.setMessages(

                  /**
                   * 1，使用匿名内部类的形式创建ArrayList的子类，
                   * 2，代码块初始化实例， add方法被继承到此类中所以无需添加前缀，、
                   *
                   * */
                  new ArrayList<RequestSSE.Message>(){

                        /**
                           *  des: 当一个类实现了 Serializable 接口以支持序列化和反序列化操作时，JVM会使用这个serialVersionUID来确保类的兼容
                         *
                         *  版本控制：当类的结构发生改变（比如增加、删除或修改了成员变量）后重新序列化对象，如果没有匹配的serialVersionUID，反序列化时可能会抛出InvalidClassException异常。通过手动设置此ID，可以在一定程度上控制类的向前和向后兼容性。
                         *
                         * 性能优化：在反序列化过程中，JVM会比较序列化时保存的serialVersionUID与当前类的serialVersionUID是否一致，如果一致则可以直接反序列化，这比反射检查类结构要快得多。
                         *
                         * 安全性增强：自动生成的serialVersionUID包含了类的结构信息，可能暴露一些不安全的细节。手动指定可以减少这种风险，尽管这不是主要的安全机制。
                           * */
                      private static final long serialVersionUID = -7988151926241837899L;
                      {
                          add(RequestSSE.Message.builder()
                                  .role(Role.user.getCode())
                                  .content("jdk中存在哪些编译器")
                                  .build());
                      }
                  }
          );
          // 请求
          ResponseSync response = session.completionsSync(request);

          log.info("测试结果：{}", JSON.toJSONString(response));
      }

        /**
           *  des: 流式调用
           * */


        @Test
        public void test_SSE_stream() throws IOException, InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            RequestSSE request = new RequestSSE();
            request.setStream(true);
            request.setMessages(new ArrayList<RequestSSE.Message>(){

                private static final long serialVersionUID = -7988151926241837892L;
                {
                    add(RequestSSE.Message.builder()
                            .role(Role.user.getCode())
                            .content("jdk中存在哪些编译器")
                            .build());
                }

            });

            session.completionsStream(request, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, @Nullable String id, @Nullable String type, String data) {
                    if ("[DONE]".equals(data)) {
                        log.info("[输出结束] Tokens {}", JSON.toJSONString(data));
                        return;
                    }

                    ResponseStream response = JSON.parseObject(data, ResponseStream.class);
                    log.info("测试结果：{}", JSON.toJSONString(response));
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    log.info("对话完成");
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                    log.error("对话失败", t);
                    countDownLatch.countDown();
                }
            });

            // 等待
            countDownLatch.await();

        }


}
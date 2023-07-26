package com.theokanning.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.reactivex.FlowableEmitter;
import okhttp3.ResponseBody;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.StreamResetException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * Callback to parse Server Sent Events (SSE) from raw InputStream and
 * emit the events with io.reactivex.FlowableEmitter to allow streaming of
 * SSE.
 */
public class ResponseBodyCallback implements Callback<ResponseBody> {
    private static final ObjectMapper mapper = OpenAiService.defaultObjectMapper();

    private FlowableEmitter<SSE> emitter;
    private boolean emitDone;

    public ResponseBodyCallback(FlowableEmitter<SSE> emitter, boolean emitDone) {
        this.emitter = emitter;
        this.emitDone = emitDone;
    }

    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        BufferedReader reader = null;

        try {
            if (!response.isSuccessful()) {
                HttpException e = new HttpException(response);
                ResponseBody errorBody = response.errorBody();

                if (errorBody == null) {
                    throw e;
                } else {
                    OpenAiError error = mapper.readValue(
                            errorBody.string(),
                            OpenAiError.class
                    );
                    throw new OpenAiHttpException(error, e, e.code());
                }
            }

            InputStream in = response.body().byteStream();
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            SSE sse = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    sse = new SSE(data);
                } else if (line.equals("") && sse != null) {
                    if (sse.isDone()) {
                        if (emitDone) {
                            emitter.onNext(sse);
                        }
                        break;
                    }

                    emitter.onNext(sse);
                    sse = null;
                } else {
                    throw new SSEFormatException("Invalid sse format! " + line);
                }
            }

            emitter.onComplete();
        } catch (Throwable t) {
            boolean isCancel = false;
            //手动取消流请求会抛出异常ErrorCode.CANCEL，所以需要手动判断为完成。
            //https://github.com/square/okhttp/issues/2964
            if (t instanceof StreamResetException) {
                StreamResetException resetException = (StreamResetException) t;
                if (resetException.errorCode.httpCode == ErrorCode.CANCEL.httpCode) {
                    isCancel = true;
                }
            }
            if (isCancel) {
                //并不会调用下游，只不过会取消从队列中继续取从而return。
                // 正常完成会，从线程池取run执行next。从而执行下游onComplete。还需要进一步研究
                emitter.onComplete();
            } else {
                onFailure(call, t);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {

        //在stream发出请求，没有接收到返回之前，手动取消调用时就会发生错误 IOException Canceled
        //https://stackoverflow.com/questions/40823134
        boolean isCancel = false;
        if (t instanceof IOException) {
            if ("Canceled".equals(t.getMessage())){
                isCancel = true;
            }
        }
        if (isCancel) {
            emitter.onComplete();
        } else {
            emitter.onError(t);
        }
    }
}
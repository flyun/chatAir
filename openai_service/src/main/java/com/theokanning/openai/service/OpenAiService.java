package com.theokanning.openai.service;

import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.flyun.base.BaseMessage;
import com.theokanning.openai.DeleteResult;
import com.theokanning.openai.GoogleError;
import com.theokanning.openai.GoogleHttpException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.CompletionChunk;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatGCompletionRequest;
import com.theokanning.openai.completion.chat.ChatGCompletionResponse;
import com.theokanning.openai.completion.chat.anthropic.AnthropicError;
import com.theokanning.openai.completion.chat.anthropic.AnthropicHttpException;
import com.theokanning.openai.completion.chat.anthropic.ChatACompletionChoice;
import com.theokanning.openai.completion.chat.anthropic.ChatACompletionRequest;
import com.theokanning.openai.completion.chat.anthropic.ChatACompletionResponse;
import com.theokanning.openai.completion.chat.anthropic.ChatAContentType;
import com.theokanning.openai.edit.EditRequest;
import com.theokanning.openai.edit.EditResult;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.file.File;
import com.theokanning.openai.finetune.FineTuneEvent;
import com.theokanning.openai.finetune.FineTuneRequest;
import com.theokanning.openai.finetune.FineTuneResult;
import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.CreateImageVariationRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiService {

    private static final String BASE_URL = "https://api.openai.com/";
    private static final String BASE_GOOGLE_URL = "https://generativelanguage.googleapis.com/";
    private static final long DEFAULT_TIMEOUT = 10;
    private static final ObjectMapper mapper = defaultObjectMapper();

    private final OpenAiApi api;
    private OkHttpClient client;
    private final ExecutorService executorService;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final WeakHashMap<String, Call<ResponseBody>> requestList = new WeakHashMap();

    public final int listModels = 1;

    private LLMType llmTypeUrl = LLMType.unKnow;
    private LLMType llmTypeToken = LLMType.unKnow;

    public static ErrorCallback errorCallback;

    private final static String URL_PREFIX = "v1";
    private String prefixUrl = URL_PREFIX;
    private final static String TEMP_PREFIX_URL = "tempPrefixUrl";
    private final static String TEMP_URL = "tempUrl";

    /**
     * Creates a new OpenAiService that wraps OpenAiApi
     *
     * @param token OpenAi token string "sk-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
     */
    public OpenAiService(final String token) {
        this(token, DEFAULT_TIMEOUT, BASE_URL, LLMType.openAi);
    }

    /**
     * Creates a new OpenAiService that wraps OpenAiApi
     *
     * @param token  OpenAi token string "sk-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
     * @param second http read timeout
     */
    public OpenAiService(final String token, final long second, final String url, LLMType llmType) {

        String tempUrl = (TextUtils.isEmpty(url) || HttpUrl.parse(url) == null) ? BASE_URL : url;

        String baseUrl = processPrefixUrl(llmType, tempUrl);

        ObjectMapper mapper = defaultObjectMapper();
        this.client = defaultClient(token, second, baseUrl, llmType);
        this.executorService = client.dispatcher().executorService();
        Retrofit retrofit = defaultRetrofit(client, mapper, baseUrl, this.executorService);

        this.api = retrofit.create(OpenAiApi.class);

        llmTypeUrl = llmType;
        llmTypeToken = llmType;
    }

    // 只切换url
    public void switchDefault(String token, String url) {

        checkLLMToken(token, LLMType.openAi);
        checkLLMServer(url, LLMType.openAi);
    }

    public void switchGoogle(String token, String url) {

        checkLLMToken(token, LLMType.google);
        checkLLMServer(url, LLMType.google);
    }

    public void switchClaude(String token, String url) {

        checkLLMToken(token, LLMType.anthropic);
        checkLLMServer(url, LLMType.anthropic);
    }

    public void switchDeepseek(String token, String url) {

        checkLLMToken(token, LLMType.deepseek);
        checkLLMServer(url, LLMType.deepseek);
    }

    public void changeMatchToken(String token, String url) {

        checkMatchToken(token, url, LLMType.openAi);

    }

    public void changeMatchServer(String url, String token) {

        checkMatchUrl(token, url, LLMType.openAi);
    }


    public void changeMatchTokenGoogle(String token, String url) {

        checkMatchToken(token, url, LLMType.google);
    }

    public void changeMatchServerGoogle(String url, String token) {

        checkMatchToken(token, url, LLMType.google);
    }

    public void changeMatchTokenClaude(String token, String url) {

        checkMatchToken(token, url, LLMType.anthropic);
    }

    public void changeMatchServerClaude(String url, String token) {

        checkMatchToken(token, url, LLMType.anthropic);
    }

    public void changeMatchTokenDeepseek(String token, String url) {

        checkMatchToken(token, url, LLMType.deepseek);
    }

    public void changeMatchServerDeepseek(String url, String token) {

        checkMatchToken(token, url, LLMType.deepseek);
    }

    public void checkLLMToken(String token, LLMType llmType) {

        if (llmTypeToken != llmType) {
            changeLLMToken(token, llmType);
        }
    }

    public void checkLLMServer(String url, LLMType llmType) {

        if (llmTypeUrl != llmType) {
            changeLLMServer(url, llmType);
        }

    }

    public void checkMatchToken(String token, String url, LLMType llmType) {

        if (llmTypeUrl == llmType) {
            // Url为自身，不需要改变
            checkLLMToken(token, llmType);
        } else {
            // Url为其他，需要Url和token一起改变
            changeLLMTokenAndServer(token, url, llmType);
        }

    }

    public void checkMatchUrl(String token, String url, LLMType llmType) {

        if (llmTypeToken == llmType) {
            // token为自身，不需要改变
            checkLLMServer(url, llmType);
        } else {
            // token为其他，需要Url和token一起改变
            changeLLMTokenAndServer(token, url, llmType);
        }

    }

    public void changeLLMToken(String token, LLMType llmType) {
        if (client != null && token != null) {
            for (Interceptor interceptor : client.interceptors()) {
                if (interceptor instanceof AuthenticationInterceptor) {
                    llmTypeToken = llmType;
                    ((AuthenticationInterceptor) interceptor).setToken(token);
                    ((AuthenticationInterceptor) interceptor).setLlmType(getLLMType(llmType));
                }
            }
        }
    }

    public void changeLLMServer(String url, LLMType llmType) {
        if (client != null && !TextUtils.isEmpty(url)) {
            for (Interceptor interceptor : client.interceptors()) {
                if (interceptor instanceof AuthenticationInterceptor) {
                    llmTypeUrl = llmType;
                    String baseUrl = processPrefixUrl(llmType, url);
                    ((AuthenticationInterceptor) interceptor).setUrl(baseUrl);
                    ((AuthenticationInterceptor) interceptor).setLlmType(getLLMType(llmType));
                }
            }
        }
    }

    public void changeLLMTokenAndServer(String token, String url, LLMType llmType) {
        if (client != null && !TextUtils.isEmpty(token) && !TextUtils.isEmpty(url)) {
            for (Interceptor interceptor : client.interceptors()) {
                if (interceptor instanceof AuthenticationInterceptor) {
                    llmTypeToken = llmType;
                    llmTypeUrl = llmType;
                    String baseUrl = processPrefixUrl(llmType, url);
                    ((AuthenticationInterceptor) interceptor).setToken(token);
                    ((AuthenticationInterceptor) interceptor).setUrl(baseUrl);
                    ((AuthenticationInterceptor) interceptor).setLlmType(getLLMType(llmType));
                }
            }
        }
    }

    public LLMType getLLMType(LLMType llmType) {
        if (llmTypeToken == llmType && llmTypeUrl == llmType) return llmType;

        System.out.print("llmType:" + llmType
                + "URL does not match token isUrl:" + llmTypeUrl + "isToken:" + llmTypeToken);
        return LLMType.unKnow;
    }

    // 解决同类型Url修改，不生效的问题
    public void resetUrlLLMType() {
        llmTypeUrl = LLMType.unKnow;
    }

    // 解决同类型Token修改，不生效的问题
    public void resetTokenLLMType() {
        llmTypeToken = LLMType.unKnow;
    }

    /**
     * Creates a new OpenAiService that wraps OpenAiApi.
     * Use this if you need more customization, but use OpenAiService(api, executorService) if
     * you use streaming and
     * want to shut down instantly
     *
     * @param api OpenAiApi instance to use for all methods
     */
    public OpenAiService(final OpenAiApi api) {
        this.api = api;
        this.executorService = null;
    }

    /**
     * Creates a new OpenAiService that wraps OpenAiApi.
     * The ExecutorService must be the one you get from the client you created the api with
     * otherwise shutdownExecutor() won't work.
     * <p>
     * Use this if you need more customization.
     *
     * @param api             OpenAiApi instance to use for all methods
     * @param executorService the ExecutorService from client.dispatcher().executorService()
     */
    public OpenAiService(final OpenAiApi api, final ExecutorService executorService) {
        this.api = api;
        this.executorService = executorService;
    }

    public List<Model> listModels() {
        return execute(api.listModels()).data;
    }

    public Model getModel(String modelId) {
        return execute(api.getModel(modelId));
    }

    public CompletionResult createCompletion(CompletionRequest request) {
        return execute(api.createCompletion(request));
    }

    public Flowable<CompletionChunk> streamCompletion(CompletionRequest request) {
        request.setStream(true);

        return stream(api.createCompletionStream(request), CompletionChunk.class);
    }

    public ChatCompletionResult createChatCompletion(ChatCompletionRequest request) {
        return execute(api.createChatCompletion(request));
    }

    public void createChatCompletion(ChatCompletionRequest request, final BaseMessage baseMessage
            , final ResultCallBack callBack) {

        createChatCompletion(api.createChatCompletion(request), baseMessage, callBack);
    }

    public void createChatGCompletion(ChatGCompletionRequest request, String model, String version,
                                      final BaseMessage baseMessage,
                                      final ResultGCallBack callBack) {

        createChatGCompletion(api.createGChatCompletion(model, version, request), baseMessage,
                callBack);
    }

    public void createChatACompletion(ChatACompletionRequest request,
                                      final ResultACallBack callBack) {

        createChatACompletion(api.createAChatCompletion(request), callBack);
    }

    public Flowable<ChatCompletionChunk> streamChatCompletion(ChatCompletionRequest request) {
        request.setStream(true);

        return stream(api.createChatCompletionStream(request), ChatCompletionChunk.class);
    }

    public EditResult createEdit(EditRequest request) {
        return execute(api.createEdit(request));
    }

    public EmbeddingResult createEmbeddings(EmbeddingRequest request) {
        return execute(api.createEmbeddings(request));
    }

    public List<File> listFiles() {
        return execute(api.listFiles()).data;
    }

    public File uploadFile(String purpose, String filepath) {
        java.io.File file = new java.io.File(filepath);
        RequestBody purposeBody = RequestBody.create(okhttp3.MultipartBody.FORM, purpose);
        RequestBody fileBody = RequestBody.create(MediaType.parse("text"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", filepath, fileBody);

        return execute(api.uploadFile(purposeBody, body));
    }

    public DeleteResult deleteFile(String fileId) {
        return execute(api.deleteFile(fileId));
    }

    public File retrieveFile(String fileId) {
        return execute(api.retrieveFile(fileId));
    }

    public FineTuneResult createFineTune(FineTuneRequest request) {
        return execute(api.createFineTune(request));
    }

    public CompletionResult createFineTuneCompletion(CompletionRequest request) {
        return execute(api.createFineTuneCompletion(request));
    }

    public List<FineTuneResult> listFineTunes() {
        return execute(api.listFineTunes()).data;
    }

    public FineTuneResult retrieveFineTune(String fineTuneId) {
        return execute(api.retrieveFineTune(fineTuneId));
    }

    public FineTuneResult cancelFineTune(String fineTuneId) {
        return execute(api.cancelFineTune(fineTuneId));
    }

    public List<FineTuneEvent> listFineTuneEvents(String fineTuneId) {
        return execute(api.listFineTuneEvents(fineTuneId)).data;
    }

    public DeleteResult deleteFineTune(String fineTuneId) {
        return execute(api.deleteFineTune(fineTuneId));
    }

    public ImageResult createImage(CreateImageRequest request) {
        return execute(api.createImage(request));
    }

    public ImageResult createImageEdit(CreateImageEditRequest request, String imagePath,
                                       String maskPath) {
        java.io.File image = new java.io.File(imagePath);
        java.io.File mask = null;
        if (maskPath != null) {
            mask = new java.io.File(maskPath);
        }
        return createImageEdit(request, image, mask);
    }

    public ImageResult createImageEdit(CreateImageEditRequest request, java.io.File image,
                                       java.io.File mask) {
        RequestBody imageBody = RequestBody.create(MediaType.parse("image"), image);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("prompt", request.getPrompt())
                .addFormDataPart("size", request.getSize())
                .addFormDataPart("model", request.getModel())
                .addFormDataPart("response_format", request.getResponseFormat())
                .addFormDataPart("image", "image", imageBody);

        if (request.getN() != null) {
            builder.addFormDataPart("n", request.getN().toString());
        }

        if (mask != null) {
            RequestBody maskBody = RequestBody.create(MediaType.parse("image"), mask);
            builder.addFormDataPart("mask", "mask", maskBody);
        }

        return execute(api.createImageEdit(builder.build()));
    }

    public ImageResult createImageVariation(CreateImageVariationRequest request, String imagePath) {
        java.io.File image = new java.io.File(imagePath);
        return createImageVariation(request, image);
    }

    public ImageResult createImageVariation(CreateImageVariationRequest request,
                                            java.io.File image) {
        RequestBody imageBody = RequestBody.create(MediaType.parse("image"), image);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
                .addFormDataPart("size", request.getSize())
                .addFormDataPart("model", request.getModel())
                .addFormDataPart("response_format", request.getResponseFormat())
                .addFormDataPart("image", "image", imageBody);

        if (request.getN() != null) {
            builder.addFormDataPart("n", request.getN().toString());
        }

        return execute(api.createImageVariation(builder.build()));
    }

    public ModerationResult createModeration(ModerationRequest request) {
        return execute(api.createModeration(request));
    }

    /**
     * Calls the Open AI api, returns the response, and parses error messages if the request fails
     */
    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();

                OpenAiError error = mapper.readValue(errorBody, OpenAiError.class);
                throw new OpenAiHttpException(error, e, e.code());
            } catch (IOException ex) {
                // couldn't parse OpenAI error
                throw e;
            }
        }
    }

    public void loading(ResultCallBack resultCallBack) {
        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        resultCallBack.onLoading(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void loadingG(ResultGCallBack resultCallBack) {
        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        resultCallBack.onLoading(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void loadingA(ResultACallBack resultCallBack) {
        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        resultCallBack.onLoading(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private Disposable streamLoadingDisposable;

    public void loading(StreamCallBack streamCallBack) {

        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        streamLoadingDisposable = d;
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if (isStreamFirstLoading) {
                            if (streamCallBack != null) streamCallBack.onLoading(true);
                        } else {
                            if (streamLoadingDisposable != null) {
                                compositeDisposable.remove(streamLoadingDisposable);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (streamLoadingDisposable != null) {
                            compositeDisposable.remove(streamLoadingDisposable);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void loadingG(StreamGCallBack streamCallBack) {

        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        streamLoadingDisposable = d;
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if (isStreamFirstLoading) {
                            if (streamGCallBack != null) streamGCallBack.onLoading(true);
                        } else {
                            if (streamLoadingDisposable != null) {
                                compositeDisposable.remove(streamLoadingDisposable);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (streamLoadingDisposable != null) {
                            compositeDisposable.remove(streamLoadingDisposable);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void loadingA(StreamACallBack streamCallBack) {

        Observable.interval(0, 5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        streamLoadingDisposable = d;
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if (isStreamFirstLoading) {
                            if (streamACallBack != null) streamACallBack.onLoading(true);
                        } else {
                            if (streamLoadingDisposable != null) {
                                compositeDisposable.remove(streamLoadingDisposable);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (streamLoadingDisposable != null) {
                            compositeDisposable.remove(streamLoadingDisposable);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public <T> void baseCompletion(int type, CompletionCallBack<T> callBack) {
        sortCompletion(type, null, callBack);
    }

    public <T> void sortCompletion(int type, CompletionRequest completionRequest,
                                   CompletionCallBack<T> callBack) {

        switch (type) {
            case listModels: {
                createCompletion(api.listModels(), callBack);
            }
        }

    }


    public <T> void createCompletion(Single<T> apiCall, CompletionCallBack<?> callBack) {

        compositeDisposable.clear();

        apiCall
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((new SingleObserver<T>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override
                    public void onSuccess(@NonNull T t) {
                        callBack.onSuccess(t);
                        compositeDisposable.clear();
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {

                        HttpException e;
                        if (throwable instanceof HttpException) {
                            e = (HttpException) throwable;
                        } else {
                            callBack.onError(null, throwable);
                            return;
                        }

                        try {
                            if (e.response() == null || e.response().errorBody() == null) {
                                callBack.onError(null, throwable);
                            } else {
                                String errorBody = e.response().errorBody().string();

                                OpenAiError error = mapper.readValue(errorBody, OpenAiError.class);
                                callBack.onError(new OpenAiHttpException(error, e, e.code()),
                                        throwable);
                            }
                        } catch (IOException ex) {
                            // couldn't parse OpenAI error
                            callBack.onError(null, throwable);
                        } finally {
                            compositeDisposable.clear();
                        }
                    }
                }));

    }

    private ResultCallBack resultCall;

    public <T> void createChatCompletion(Single<T> apiCall, BaseMessage baseMessage,
                                         ResultCallBack resultCallBack) {

        compositeDisposable.clear();

        apiCall
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((new SingleObserver<T>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                        resultCall = resultCallBack;
                        compositeDisposable.add(disposable);
                        loading(resultCallBack);
                    }

                    @Override
                    public void onSuccess(@NonNull T t) {
                        if (t instanceof ChatCompletionResult) {
                            resultCallBack.onSuccess((ChatCompletionResult) t);
                        }
                        resultCallBack.onLoading(false);
                        compositeDisposable.clear();
                        resultCall = null;
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {

                        HttpException e;
                        if (throwable instanceof HttpException) {
                            e = (HttpException) throwable;
                        } else {
                            resultCallBack.onError(null, throwable);
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultCall = null;
                            return;
                        }

                        try {
                            if (e.response() == null || e.response().errorBody() == null) {
                                resultCallBack.onError(null, throwable);
                            } else {
                                String errorBody = e.response().errorBody().string();

                                OpenAiError error = mapper.readValue(errorBody, OpenAiError.class);
                                if (error != null && error.error != null) {
                                    resultCallBack.onError(new OpenAiHttpException(error, e,
                                                    e.code()),
                                            throwable);
                                } else {
                                    resultCallBack.onError(null, throwable);
                                }

                            }
                        } catch (IOException ex) {
                            // couldn't parse OpenAI error
                            resultCallBack.onError(null, throwable);
                        } finally {
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultCall = null;
                        }

                    }
                }));

    }

    private ResultGCallBack resultGCall;

    public <T> void createChatGCompletion(Single<T> apiCall, BaseMessage baseMessage,
                                          ResultGCallBack resultCallBack) {

        compositeDisposable.clear();

        apiCall
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((new SingleObserver<T>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                        resultGCall = resultCallBack;
                        compositeDisposable.add(disposable);
                        loadingG(resultCallBack);
                    }

                    @Override
                    public void onSuccess(@NonNull T t) {
                        if (t instanceof ChatGCompletionResponse) {
                            resultCallBack.onSuccess((ChatGCompletionResponse) t);
                        }
                        resultCallBack.onLoading(false);
                        compositeDisposable.clear();
                        resultGCall = null;
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {


                        HttpException e;
                        if (throwable instanceof HttpException) {
                            e = (HttpException) throwable;
                        } else {
                            resultCallBack.onError(null, throwable);
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultGCall = null;
                            return;
                        }

                        try {
                            if (e.response() == null || e.response().errorBody() == null) {
                                resultCallBack.onError(null, throwable);
                            } else {
                                String errorBody = e.response().errorBody().string();

                                GoogleError error = mapper.readValue(errorBody, GoogleError.class);
                                resultCallBack.onError(new GoogleHttpException(error, throwable),
                                        throwable);
                            }
                        } catch (IOException ex) {
                            // couldn't parse OpenAI error
                            resultCallBack.onError(null, throwable);
                        } finally {
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultGCall = null;
                        }

                    }
                }));

    }

    private ResultACallBack resultACall;

    public <T> void createChatACompletion(Single<T> apiCall, ResultACallBack resultCallBack) {

        compositeDisposable.clear();

        apiCall
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((new SingleObserver<T>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                        resultACall = resultCallBack;
                        compositeDisposable.add(disposable);
                        loadingA(resultCallBack);
                    }

                    @Override
                    public void onSuccess(@NonNull T t) {
                        if (t instanceof ChatACompletionResponse) {
                            resultCallBack.onSuccess((ChatACompletionResponse) t);
                        }
                        resultCallBack.onLoading(false);
                        compositeDisposable.clear();
                        resultACall = null;
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {


                        HttpException e;
                        if (throwable instanceof HttpException) {
                            e = (HttpException) throwable;
                        } else {
                            resultCallBack.onError(null, throwable);
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultACall = null;
                            return;
                        }

                        try {
                            if (e.response() == null || e.response().errorBody() == null) {
                                resultCallBack.onError(null, throwable);
                            } else {
                                String errorBody = e.response().errorBody().string();

                                AnthropicError error = mapper.readValue(errorBody,
                                        AnthropicError.class);
                                resultCallBack.onError(new AnthropicHttpException(error, throwable),
                                        throwable);
                            }
                        } catch (IOException ex) {
                            // couldn't parse OpenAI error
                            resultCallBack.onError(null, throwable);
                        } finally {
                            resultCallBack.onLoading(false);
                            compositeDisposable.clear();
                            resultACall = null;
                        }

                    }
                }));

    }

    private volatile boolean isStreamFirstLoading;
    private StreamCallBack streamCallBack;

    public void streamChatCompletion(ChatCompletionRequest request, StreamCallBack callBack) {

        request.setStream(true);
        isStreamFirstLoading = true;
        loading(callBack);
        //因为on在取消stream请求的问题（具体看ResponseBodyCallback），无法执行onCompletion，所以需要折中调用onCompletion
        streamCallBack = callBack;

        Call<ResponseBody> apiCall = api.createChatCompletionStream(request);
        Disposable disposable = stream(apiCall, ChatCompletionChunk.class)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .forEachWhile(chatCompletionChunk -> {

                            if (isStreamFirstLoading) {
                                isStreamFirstLoading = false;
                                if (streamCallBack != null) streamCallBack.onLoading(false);
                            }

                            if (streamCallBack != null) streamCallBack.onSuccess(chatCompletionChunk);

                            //返回true代表继续回调，false代表执行完成回调
                            return chatCompletionChunk.getChoices() == null
                                    || chatCompletionChunk.getChoices().size() <= 0
                                    || chatCompletionChunk.getChoices().get(0).getFinishReason() == null;
                        },
                        throwable -> {

                            HttpException e;
                            if (throwable instanceof HttpException) {
                                e = (HttpException) throwable;
                            } else {
                                if (streamCallBack != null) streamCallBack.onError(null, throwable);

                                if (streamCallBack != null) streamCallBack.onLoading(false);
                                streamCallBack = null;
                                compositeDisposable.clear();

                                return;
                            }

                            try {
                                if (e.response() == null || e.response().errorBody() == null) {
                                    if (streamCallBack != null)
                                        streamCallBack.onError(null, throwable);
                                } else {
                                    String errorBody = e.response().errorBody().string();

                                    OpenAiError error = mapper.readValue(errorBody,
                                            OpenAiError.class);
                                    if (streamCallBack != null)
                                        streamCallBack.onError(new OpenAiHttpException(error, e,
                                                        e.code()),
                                                throwable);
                                }
                            } catch (IOException ex) {
                                // couldn't parse OpenAI error
                                if (streamCallBack != null) streamCallBack.onError(null, throwable);
                            } finally {
                                if (streamCallBack != null) streamCallBack.onLoading(false);
                                streamCallBack = null;
                                compositeDisposable.clear();
                            }
                        },
                        () -> {
                            if (streamCallBack != null) streamCallBack.onCompletion();
                            streamCallBack = null;
                            compositeDisposable.clear();
                        });

        compositeDisposable.add(disposable);
        requestList.put("streamChatCompletion", apiCall);
    }


    private StreamGCallBack streamGCallBack;

    public void streamChatGCompletion(ChatGCompletionRequest request, String model, String version,
                                      StreamGCallBack callBack) {

        isStreamFirstLoading = true;
        loadingG(callBack);
        //因为on在取消stream请求的问题（具体看ResponseBodyCallback），无法执行onCompletion，所以需要折中调用onCompletion
        streamGCallBack = callBack;

        Call<ResponseBody> apiCall = api.createGChatCompletionStream(model, version, request);
        Disposable disposable = streamG(apiCall, ChatGCompletionResponse.class)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .forEachWhile(ChatGCompletionResponse -> {

                            if (isStreamFirstLoading) {
                                isStreamFirstLoading = false;
                                if (streamGCallBack != null) streamGCallBack.onLoading(false);
                            }

                            if (streamGCallBack != null) streamGCallBack.onSuccess(ChatGCompletionResponse);

                            //返回true代表继续回调，false代表执行完成回调
                            return ChatGCompletionResponse.getCandidates() == null
                                    || ChatGCompletionResponse.getCandidates().size() <= 0
                                    || ChatGCompletionResponse.getCandidates().get(0).getContent() == null
                                    || ChatGCompletionResponse.getCandidates().get(0).getContent().getParts() == null
                                    || ChatGCompletionResponse.getCandidates().get(0).getContent().getParts().size() <= 0
                                    || ChatGCompletionResponse.getCandidates().get(0).getContent().getParts().get(0) == null
                                    || !ChatGCompletionResponse.getCandidates().get(0).getContent().getParts().get(0).getText().equals("")
                                    ;
                        },
                        throwable -> {

                            HttpException e;
                            if (throwable instanceof HttpException) {
                                e = (HttpException) throwable;
                            } else {
                                if (streamGCallBack != null)
                                    streamGCallBack.onError(null, throwable);
                                return;
                            }

                            try {
                                if (e.response() == null || e.response().errorBody() == null) {
                                    if (streamGCallBack != null)
                                        streamGCallBack.onError(null, throwable);
                                } else {
                                    String errorBody = e.response().errorBody().string();

                                    GoogleError error = mapper.readValue(errorBody,
                                            GoogleError.class);
                                    if (streamGCallBack != null)
                                        streamGCallBack.onError(new GoogleHttpException(error, e),
                                                throwable);
                                }
                            } catch (IOException ex) {
                                // couldn't parse OpenAI error
                                if (streamGCallBack != null)
                                    streamGCallBack.onError(null, throwable);
                            } finally {
                                if (streamGCallBack != null) streamGCallBack.onLoading(false);
                                streamGCallBack = null;
                                compositeDisposable.clear();
                            }
                        },
                        () -> {
                            if (streamGCallBack != null) streamGCallBack.onCompletion();
                            streamGCallBack = null;
                            compositeDisposable.clear();
                        });

        compositeDisposable.add(disposable);
        requestList.put("streamChatCompletion", apiCall);
    }

    private StreamACallBack streamACallBack;

    public void streamChatACompletion(ChatACompletionRequest request, StreamACallBack callBack) {

        request.setStream(true);
        isStreamFirstLoading = true;
        loadingA(callBack);
        //因为on在取消stream请求的问题（具体看ResponseBodyCallback），无法执行onCompletion，所以需要折中调用onCompletion
        streamACallBack = callBack;

        Call<ResponseBody> apiCall = api.createAChatCompletionStream(request);
        Disposable disposable = streamA(apiCall, ChatACompletionChoice.class)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .forEachWhile(chatCompletionChunk -> {

                            if (isStreamFirstLoading) {
                                isStreamFirstLoading = false;
                                if (streamACallBack != null) streamACallBack.onLoading(false);
                            }

                            if (streamACallBack != null) streamACallBack.onSuccess(chatCompletionChunk);

                            //返回true代表继续回调，false代表执行完成回调
                            return !ChatAContentType.MESSAGE_STOP.value().equals(chatCompletionChunk.getType())
//                            return chatCompletionChunk.getContent() == null
//                                    || chatCompletionChunk.getContent().size() <= 0
//                                    || chatCompletionChunk.getContent().get(0).getFinishReason
//                                    () == null
                                    ;
                        },
                        throwable -> {

                            HttpException e;
                            if (throwable instanceof HttpException) {
                                e = (HttpException) throwable;
                            } else {
                                if (streamACallBack != null)
                                    streamACallBack.onError(null, throwable);

                                if (streamACallBack != null) streamACallBack.onLoading(false);
                                streamACallBack = null;
                                compositeDisposable.clear();

                                return;
                            }

                            try {
                                if (e.response() == null || e.response().errorBody() == null) {
                                    if (streamACallBack != null)
                                        streamACallBack.onError(null, throwable);
                                } else {
                                    String errorBody = e.response().errorBody().string();

                                    AnthropicError error = mapper.readValue(errorBody,
                                            AnthropicError.class);
                                    if (streamACallBack != null)
                                        streamACallBack.onError(new AnthropicHttpException(error,
                                                        e),
                                                throwable);
                                }
                            } catch (IOException ex) {
                                // couldn't parse OpenAI error
                                if (streamACallBack != null)
                                    streamACallBack.onError(null, throwable);
                            } finally {
                                if (streamACallBack != null) streamACallBack.onLoading(false);
                                streamACallBack = null;
                                compositeDisposable.clear();
                            }
                        },
                        () -> {
                            if (streamACallBack != null) streamACallBack.onCompletion();
                            streamACallBack = null;
                            compositeDisposable.clear();
                        });

        compositeDisposable.add(disposable);
        requestList.put("streamAChatCompletion", apiCall);
    }

    public interface StreamCallBack {

        void onSuccess(ChatCompletionChunk result);

        void onError(OpenAiHttpException error, Throwable Throwable);

        void onCompletion();

        void onLoading(boolean isLoading);

    }

    public interface StreamGCallBack {

        void onSuccess(ChatGCompletionResponse result);

        void onError(GoogleHttpException error, Throwable Throwable);

        void onCompletion();

        void onLoading(boolean isLoading);

    }

    public interface StreamACallBack {

        void onSuccess(ChatACompletionChoice result);

        void onError(AnthropicHttpException error, Throwable Throwable);

        void onCompletion();

        void onLoading(boolean isLoading);

    }


    public interface CompletionCallBack<T> {
        void onSuccess(Object o);

        void onError(OpenAiHttpException error, Throwable Throwable);
    }

    public interface ResultCallBack {

        void onSuccess(ChatCompletionResult result);

        void onError(OpenAiHttpException error, Throwable Throwable);

        void onLoading(boolean isLoading);

    }

    public interface ResultGCallBack {

        void onSuccess(ChatGCompletionResponse result);

        void onError(GoogleHttpException error, Throwable Throwable);

        void onLoading(boolean isLoading);

    }

    public interface ResultACallBack {

        void onSuccess(ChatACompletionResponse result);

        void onError(AnthropicHttpException error, Throwable Throwable);

        void onLoading(boolean isLoading);

    }


    /**
     * Calls the Open AI api and returns a Flowable of SSE for streaming
     * omitting the last message.
     *
     * @param apiCall The api call
     */
    public static Flowable<SSE> stream(Call<ResponseBody> apiCall) {
        return stream(apiCall, false);
    }

    public static Flowable<SSE> streamG(Call<ResponseBody> apiCall) {
        return streamG(apiCall, false);
    }

    public static Flowable<SSE> streamA(Call<ResponseBody> apiCall) {
        return streamA(apiCall, false);
    }

    /**
     * Calls the Open AI api and returns a Flowable of SSE for streaming.
     *
     * @param apiCall  The api call
     * @param emitDone If true the last message ([DONE]) is emitted
     */
    public static Flowable<SSE> stream(Call<ResponseBody> apiCall, boolean emitDone) {
        //apiCall.enqueue为retrofit手动调用okHttp，所以默认情况下，在Android平台，因为平台判断
        //所以默认回调在主线程，但是这里ResponseBodyCallback内部进行数据读写、网络等耗时操作，需要在子线程进行操作
        //方法有两种，可以在retrofit传入线程池，或者ResponseBodyCallback回调中切换到子线程
        return Flowable.create(emitter -> apiCall.enqueue(new ResponseBodyCallback(emitter,
                emitDone)), BackpressureStrategy.BUFFER);
    }

    public static Flowable<SSE> streamG(Call<ResponseBody> apiCall, boolean emitDone) {
        //apiCall.enqueue为retrofit手动调用okHttp，所以默认情况下，在Android平台，因为平台判断
        //所以默认回调在主线程，但是这里ResponseBodyCallback内部进行数据读写、网络等耗时操作，需要在子线程进行操作
        //方法有两种，可以在retrofit传入线程池，或者ResponseBodyCallback回调中切换到子线程
        return Flowable.create(emitter -> apiCall.enqueue(new ResponseBodyGCallback(emitter,
                emitDone)), BackpressureStrategy.BUFFER);
    }

    public static Flowable<SSE> streamA(Call<ResponseBody> apiCall, boolean emitDone) {
        //apiCall.enqueue为retrofit手动调用okHttp，所以默认情况下，在Android平台，因为平台判断
        //所以默认回调在主线程，但是这里ResponseBodyCallback内部进行数据读写、网络等耗时操作，需要在子线程进行操作
        //方法有两种，可以在retrofit传入线程池，或者ResponseBodyCallback回调中切换到子线程
        return Flowable.create(emitter -> apiCall.enqueue(new ResponseBodyACallback(emitter,
                emitDone)), BackpressureStrategy.BUFFER);
    }

    /**
     * Calls the Open AI api and returns a Flowable of type T for streaming
     * omitting the last message.
     *
     * @param apiCall The api call
     * @param cl      Class of type T to return
     */
    public static <T> Flowable<T> stream(Call<ResponseBody> apiCall, Class<T> cl) {

        return stream(apiCall).map(sse -> mapper.readValue(sse.getData(), cl));
    }

    public static <T> Flowable<T> streamG(Call<ResponseBody> apiCall, Class<T> cl) {

        return streamG(apiCall).map(sse -> mapper.readValue(sse.getData(), cl));
    }

    public static <T> Flowable<T> streamA(Call<ResponseBody> apiCall, Class<T> cl) {

        return streamA(apiCall).map(sse -> mapper.readValue(sse.getData(), cl));
    }

    public void clearRequest() {

        //todo rxJava的通用网络请求取消未做
        for (Map.Entry<String, Call<ResponseBody>> entry : requestList.entrySet()) {
            Call<ResponseBody> call = entry.getValue();

            if (call != null && call.isExecuted() && !call.isCanceled()) {
                call.cancel();
            }
        }
        requestList.clear();
    }

    public void clean() {
        clean(true, false);
    }

    public void clean(boolean isExecutor, boolean noCancelCallback) {
        clearRequest();
        if (streamCallBack != null) {
            streamCallBack.onLoading(false);
            if (streamCallBack != null) streamCallBack.onCompletion();
        }
        if (streamGCallBack != null) {
            streamGCallBack.onLoading(false);
            if (streamGCallBack != null) streamGCallBack.onCompletion();
        }
        if (streamACallBack != null) {
            streamACallBack.onLoading(false);
            if (streamACallBack != null) streamACallBack.onCompletion();
        }

        if (resultCall != null) {
            resultCall.onLoading(false);
            resultCall = null;
        }
        if (resultGCall != null) {
            resultGCall.onLoading(false);
            resultGCall = null;
        }
        if (resultACall != null) {
            resultACall.onLoading(false);
            resultACall = null;
        }

        if (!noCancelCallback) compositeDisposable.clear();
        if (isExecutor) shutdownExecutor();
    }

    /**
     * Shuts down the OkHttp ExecutorService.
     * The default behaviour of OkHttp's ExecutorService (ConnectionPool)
     * is to shut down after an idle timeout of 60s.
     * Call this method to shut down the ExecutorService immediately.
     */
    public void shutdownExecutor() {
        Objects.requireNonNull(this.executorService, "executorService must be set in order to " +
                "shut down");
        this.executorService.shutdown();
    }

    public static String formatUrl(String url) {

        // 排除问号后缀
        String formatUrl = "";

        try {
            URI uri = new URI(url);

            // 适配baseUrl
            String path = uri.getPath();
            if (path == null || !path.endsWith("/")) {
                path += "/";
            }

            // 移除问号
            formatUrl = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    path, null, null).toString();

        } catch (URISyntaxException e) {
            if (errorCallback != null) {
                errorCallback.onError(e);
            }
        }

        return formatUrl;
    }

    public String processPrefixUrl(LLMType llmType, String url) {

        HashMap<String, String> tempHashMap = null;

        this.prefixUrl = URL_PREFIX;

        switch (llmType) {
            case openAi:
            case deepseek:
                tempHashMap = processOpenAIPrefixUrl(url);
                break;
            case google:
                break;
            case anthropic:
                tempHashMap = processAnthropicPrefixUrl(url);
                break;
        }

        String tempUrl = "";
        String tempPrefixUrl = "";

        if (tempHashMap == null) {
            return url;
        }

        tempUrl = tempHashMap.get(TEMP_URL);
        tempPrefixUrl = tempHashMap.get(TEMP_PREFIX_URL);


        if (tempUrl == null || tempUrl.isEmpty()) {
            return url;
        }

        if (tempPrefixUrl != null) {
            this.prefixUrl = tempPrefixUrl;
        }

        return tempUrl;

    }

    private HashMap<String, String> processOpenAIPrefixUrl(String url) {
        String pathEnd = "chat/completions/";
        String fixPrefixUrl = "v1";

        String tempUrl = "";
        String tempPrefixUrl = "";

        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            if (path.endsWith(pathEnd)) {
                // 匹配到特定格式，拆分后缀
                tempPrefixUrl = path.substring(0, path.length() - pathEnd.length());

                tempUrl = new URI(uri.getScheme(),null, uri.getHost(), uri.getPort(), tempPrefixUrl,
                        null,null).toString();
            } else {
                tempUrl = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path + "v1/",
                        null,null).toString();
                tempPrefixUrl = fixPrefixUrl;
            }
        } catch (URISyntaxException e) {
            tempUrl = "";
            tempPrefixUrl = fixPrefixUrl;
            if (errorCallback != null) errorCallback.onError(e);
        }

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(TEMP_URL, tempUrl);
        hashMap.put(TEMP_PREFIX_URL, tempPrefixUrl);

        return hashMap;
    }

    private HashMap<String, String> processAnthropicPrefixUrl(String url) {
        String pathEnd = "messages/";
        String fixPrefixUrl = URL_PREFIX;

        String tempUrl = "";
        String tempPrefixUrl = "";

        try {
            URI uri = new URI(url);
            HttpUrl httpUrl = HttpUrl.parse(url);
            String path = uri.getPath();
            int port = -1;
            if (httpUrl != null) {
                port = httpUrl.port();
            }

            if (path.endsWith(pathEnd)) {
                // 匹配到特定格式，拆分后缀
                tempPrefixUrl = path.substring(0, path.length() - pathEnd.length());

                tempUrl = new URI(uri.getScheme(), null, uri.getHost(), port,
                        tempPrefixUrl, null, null).toString();

            } else {

                tempUrl = new URI(uri.getScheme(), null, uri.getHost(), port,
                        "/v1/", null, null).toString();

                tempPrefixUrl = fixPrefixUrl;
            }
        } catch (URISyntaxException e) {
            tempUrl = "";
            tempPrefixUrl = fixPrefixUrl;
            if (errorCallback != null) errorCallback.onError(e);
        }

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(TEMP_URL, tempUrl);
        hashMap.put(TEMP_PREFIX_URL, tempPrefixUrl);

        return hashMap;
    }

//    public static OpenAiApi buildApi(String token, long second) {
//        ObjectMapper mapper = defaultObjectMapper();
//        OkHttpClient client = defaultClient(token, second, BASE_URL, LLMType.openAi);
//        Retrofit retrofit = defaultRetrofit(client, mapper, BASE_URL,
//                client.dispatcher().executorService());
//
//        return retrofit.create(OpenAiApi.class);
//    }

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    public static OkHttpClient defaultClient(String token, long second, String url,
                                             LLMType llmType) {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(s -> {
//            Log.i("test",s);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(new AuthenticationInterceptor(token, url, llmType, errorCallback))
//                .addInterceptor(loggingInterceptor)
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(second * 1000, TimeUnit.MILLISECONDS)
                .build();
    }

    public static Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper, String url,
                                           Executor executor) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .callbackExecutor(executor)
                .build();
    }

    public interface ErrorCallback {
        void onError(Throwable t);
    }
}
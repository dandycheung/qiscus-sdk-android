/**
 * Copyright (c) 2016 Qiscus.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qiscus.sdk.chat.core;

import android.app.AlarmManager;
import android.app.Application;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.text.format.DateUtils;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.local.QiscusDataStore;
import com.qiscus.sdk.chat.core.data.local.QiscusDataManagement;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.model.QiscusCoreChatConfig;
import com.qiscus.sdk.chat.core.data.model.QiscusRefreshToken;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.event.QiscusInitWithCustomServerEvent;
import com.qiscus.sdk.chat.core.event.QiscusRefreshTokenEvent;
import com.qiscus.sdk.chat.core.event.QiscusUserEvent;
import com.qiscus.sdk.chat.core.service.QiscusNetworkCheckerJobService;
import com.qiscus.sdk.chat.core.service.QiscusSyncJobService;
import com.qiscus.sdk.chat.core.service.QiscusSyncService;
import com.qiscus.sdk.chat.core.util.BuildVersionUtil;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;
import com.qiscus.sdk.chat.core.util.QiscusDateUtil;
import com.qiscus.sdk.chat.core.util.QiscusErrorLogger;
import com.qiscus.sdk.chat.core.util.QiscusLogger;
import com.qiscus.sdk.chat.core.util.QiscusServiceUtil;
import com.qiscus.utils.jupukdata.JupukData;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Jul, Wed 25 2018 15.35
 **/
public class QiscusCore {
    private static JSONObject customHeader;
    private static QiscusCoreChatConfig config = QiscusCoreChatConfig.getInstance();
    private static QiscusAppComponent appComponent = QiscusAppComponent.create();
    
    private QiscusCore() {
    }

    /**
     * The first method you need to be invoke to using qiscus sdk. Call this method from your Application
     * class. You can not using another qiscus feature if you not invoke this method first. Here sample
     * to call this method:
     * <pre>
     * {@code
     * public class SampleApps extends Application {
     *  public void onCreate() {
     *      super.onCreate();
     *      QiscusCore.init(this, "yourQiscusAppId");
     *  }
     * }
     * }
     * </pre>
     *
     * @param application Application instance
     * @param qiscusAppId Your qiscus application Id
     */
    @Deprecated
    public static void init(Application application, String qiscusAppId) {
        initWithCustomServer(application, qiscusAppId, BuildConfig.BASE_URL_SERVER,
                BuildConfig.BASE_URL_MQTT_BROKER, true, BuildConfig.BASE_URL_MQTT_LB);
    }

    /**
     * The first method you need to be invoke to using qiscus sdk. Call this method from your Application
     * class. You can not using another qiscus feature if you not invoke this method first. Here sample
     * to call this method:
     * <pre>
     * {@code
     * public class SampleApps extends Application {
     *  public void onCreate() {
     *      super.onCreate();
     *      QiscusCore.setup(this, "yourQiscusAppId");
     *  }
     * }
     * }
     * </pre>
     *
     * @param application Application instance
     * @param appID       Your qiscus application Id
     */
    public static void setup(Application application, String appID) {
        initWithCustomServer(application, appID, BuildConfig.BASE_URL_SERVER,
                BuildConfig.BASE_URL_MQTT_BROKER, true, BuildConfig.BASE_URL_MQTT_LB);
    }

    /**
     * The first method you need to be invoke to using qiscus sdk. Call this method from your Application
     * class. You can not using another qiscus feature if you not invoke this method first. Here sample
     * to call this method:
     * <pre>
     * {@code
     * public class SampleApps extends Application {
     *  public void onCreate() {
     *      super.onCreate();
     *      QiscusCore.initWithCustomServer(this, my-app-id, "http://myserver.com/", "ssl://mqtt.myserver.com:1885");
     *  }
     * }
     * }
     * </pre>
     *
     * @param application Application instance
     * @param appId       Your Qiscus App Id
     * @param baseUrl     Your qiscus chat engine base url
     * @param brokerUrl   Your Mqtt Broker url
     */
    @Deprecated
    public static void initWithCustomServer(Application application, String appId, String baseUrl,
                                            String brokerUrl, String brokerLBUrl) {
        if (brokerLBUrl == null) {
            initWithCustomServer(application, appId, baseUrl, brokerUrl, false, brokerLBUrl);
        } else {
            initWithCustomServer(application, appId, baseUrl, brokerUrl, true, brokerLBUrl);
        }
    }

    /**
     * The first method you need to be invoke to using qiscus sdk. Call this method from your Application
     * class. You can not using another qiscus feature if you not invoke this method first. Here sample
     * to call this method:
     * <pre>
     * {@code
     * public class SampleApps extends Application {
     *  public void onCreate() {
     *      super.onCreate();
     *      QiscusCore.initWithCustomServer(this, my-app-id, "http://myserver.com/", "ssl://mqtt.myserver.com:1885");
     *  }
     * }
     * }
     * </pre>
     *
     * @param application Application instance
     * @param appId       Your Qiscus App Id
     * @param baseUrl     Your qiscus chat engine base url
     * @param brokerUrl   Your Mqtt Broker url
     */

    public static void setupWithCustomServer(Application application, String appId, String baseUrl,
                                             String brokerUrl, String brokerLBUrl) {
        if (brokerLBUrl == null) {
            initWithCustomServer(application, appId, baseUrl, brokerUrl, false, brokerLBUrl);
        } else {
            initWithCustomServer(application, appId, baseUrl, brokerUrl, true, brokerLBUrl);
        }
    }

    /**
     * This method @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
     *
     * @param application   Application instance
     * @param qiscusAppId   Your Qiscus App Id
     * @param serverBaseUrl Your qiscus chat engine base url
     * @param mqttBrokerUrl Your Mqtt Broker url
     * @param enableMqttLB  Qiscus using own MQTT Load Balancer for get mqtt server url
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void initWithCustomServer(Application application, String qiscusAppId, String serverBaseUrl,
                                            String mqttBrokerUrl, boolean enableMqttLB, String baseURLLB) {

        appComponent.setup(application, qiscusAppId, serverBaseUrl);
        appComponent.setEnableMqttLB(enableMqttLB);
        appComponent.setMqttBrokerUrl(mqttBrokerUrl);
        appComponent.setBaseURLLB(baseURLLB);
        appComponent.setEnableEventReport(false);

        appComponent.getLocalDataManager()
                .setURLLB(baseURLLB);

        AlarmManager alarmMgr = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmMgr.canScheduleExactAlarms()) {
                appComponent.setForceDisableRealtimeFromExactAlarm(true);
            } else {
                appComponent.setForceDisableRealtimeFromExactAlarm(false);
            }

        }
       // QiscusActivityCallback.INSTANCE.setAppActiveOrForground();
        BackgroundForegroundListener.INSTANCE.setAppActiveOrForground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkExactAlarm(application);
        }

        getAppConfig();
        //configureFcmToken();
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    private static void checkExactAlarm(Application application){
        PackageManager.OnChecksumsReadyListener check = ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {

            AlarmManager alarmMgr = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
            if (!alarmMgr.canScheduleExactAlarms()) {
                appComponent.setForceDisableRealtimeFromExactAlarm(true);

                if (QiscusPusherApi.getInstance().isConnected()) {
                    QiscusPusherApi.getInstance().disconnect();
                }
            } else {
                appComponent.setForceDisableRealtimeFromExactAlarm(false);
            }
        };
    }

    public static void isBuiltIn(Boolean isBuiltInSDK) {
        appComponent.setIsBuiltIn(isBuiltInSDK);
    }

    private static void getAppConfig() {
        QiscusApi.getInstance()
                .getAppConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appConfig -> {
                    appComponent.setEnableEventReport(appConfig.getEnableEventReport());
                    if (!appConfig.getBaseURL().isEmpty()) {
                        String oldAppServer = appComponent.getAppServer();
                        String newAppServer = !appConfig.getBaseURL().endsWith("/") ?
                                appConfig.getBaseURL() + "/" : appConfig.getBaseURL();

                        if (!oldAppServer.equals(newAppServer) &&
                                QiscusServiceUtil.isValidUrl(newAppServer)) {
                            appComponent.setAppServer(
                                    newAppServer
                            );
                        }
                    }

                    QiscusApi.getInstance().reInitiateInstance();

                    if (!appConfig.getBrokerLBURL().isEmpty() &&
                            QiscusServiceUtil.isValidUrl(appConfig.getBrokerLBURL())) {
                        appComponent.setBaseURLLB(appConfig.getBrokerLBURL());
                        appComponent.getLocalDataManager()
                                .setURLLB(appConfig.getBrokerLBURL());
                    }

                    if (!appConfig.getBrokerURL().isEmpty()) {

                        String oldMqttBrokerUrl = appComponent.getMqttBrokerUrl();
                        String newMqttBrokerUrl = String.format("ssl://%s:1885",
                                appConfig.getBrokerURL());

                        if (!oldMqttBrokerUrl.equals(newMqttBrokerUrl)) {
                            appComponent.setMqttBrokerUrl(newMqttBrokerUrl);
                            QiscusCore.setCacheMqttBrokerUrl(newMqttBrokerUrl, false);
                        } else {
                            QiscusCore.setCacheMqttBrokerUrl(appComponent.getMqttBrokerUrl(), false);
                        }
                    }

                    if (appConfig.getSyncInterval() != 0) {
                        appComponent.setHeartBeat(appConfig.getSyncInterval());
                    }

                    if (appConfig.getSyncOnConnect() != 0) {
                        appComponent.setAutomaticHeartBeat(appConfig.getSyncOnConnect());
                    }

                    if (appConfig.getNetworkConnectionInterval() != 0) {
                        appComponent.setNetworkConnectionInterval(appConfig.getNetworkConnectionInterval());
                    }

                    appComponent.setEnableRealtime(appConfig.getEnableRealtime());
                    appComponent.setEnableSync(appConfig.getEnableSync());
                    appComponent.setEnableSyncEvent(appConfig.getEnableSyncEvent());

                    //enableRefreshToken
                    appComponent.setEnableRefreshToken(appConfig.getIsEnableRefreshToken());

                    // call refresh token
                    appComponent.setAutoRefreshToken(appConfig.getAutoRefreshToken());
                    if (appConfig.getAutoRefreshToken()) {
                        autoRefreshToken();
                    }

                    startSyncService();
                    startNetworkCheckerService();
                    //ProcessLifecycleOwner.get().getLifecycle().addObserver(QiscusActivityCallback.INSTANCE);
                    ProcessLifecycleOwner.get().getLifecycle().addObserver(BackgroundForegroundListener.INSTANCE);
                }, throwable -> {
                    QiscusErrorLogger.print(throwable);
                    QiscusApi.getInstance().reInitiateInstance();

                    // call refresh token
                    autoRefreshToken();

                    QiscusCore.setCacheMqttBrokerUrl(appComponent.getMqttBrokerUrl(), false);
                    startSyncService();
                    startNetworkCheckerService();
                    //ProcessLifecycleOwner.get().getLifecycle().addObserver(QiscusActivityCallback.INSTANCE);
                    ProcessLifecycleOwner.get().getLifecycle().addObserver(BackgroundForegroundListener.INSTANCE);
                });

    }

    /**
     * Use this method to start sync service from qiscus
     */

    public static void startSyncService() {
        appComponent.setSyncServiceDisabled(false);
        checkAppIdSetup();
        Application appInstance = QiscusCore.getApps();
        if (BuildVersionUtil.isOreoLower()) {
            try {
                appInstance.getApplicationContext()
                        .startService(new Intent(appInstance.getApplicationContext(), QiscusSyncService.class));
            } catch (IllegalStateException e) {
                //Prevent crash because trying to start service while application on background
                QiscusErrorLogger.print(e);
            } catch (RuntimeException e) {
                //Prevent crash because trying to start service while application on background
                QiscusErrorLogger.print(e);
            }
        } else {
            try {
                appInstance.getApplicationContext()
                        .startService(new Intent(appInstance.getApplicationContext(), QiscusSyncJobService.class));
            } catch (IllegalStateException e) {
                //Prevent crash because trying to start service while application on background
                QiscusErrorLogger.print(e);
            } catch (RuntimeException e) {
                //Prevent crash because trying to start service while application on background
                QiscusErrorLogger.print(e);
            }
        }
    }

    /**
     * Use this method to stop sync service from qiscus
     *
     * @WARNING : when this method used, we can't restart mqtt automatically if there
     * are any problem, and we can't get message from sync if mqtt down
     */

    public static void stopSyncService() {
        appComponent.setSyncServiceDisabled(true);
        if (BuildVersionUtil.isOreoLower()) {
            try {
                getApps().getApplicationContext()
                        .stopService(new Intent(getApps().getApplicationContext(), QiscusSyncService.class));
            } catch (RuntimeException e) {
                //Prevent runtime crash because trying to stop service
                appComponent.setSyncServiceDisabled(false);
                QiscusErrorLogger.print(e);
            }
            catch (Exception e) {
                //Prevent crash because trying to stop service
                appComponent.setSyncServiceDisabled(false);
                QiscusErrorLogger.print(e);
            }
        } else {
            try {
                getApps().getApplicationContext()
                        .stopService(new Intent(getApps().getApplicationContext(), QiscusSyncJobService.class));
            } catch (RuntimeException e) {
                //Prevent runtime crash because trying to stop service
                appComponent.setSyncServiceDisabled(false);
                QiscusErrorLogger.print(e);
            } catch (Exception e) {
                //Prevent crash because trying to stop service
                appComponent.setSyncServiceDisabled(false);
                QiscusErrorLogger.print(e);
            }
        }
    }

    /**
     * start network checker job service if in oreo or higher
     */
    private static void startNetworkCheckerService() {
        if (appComponent.getAppServer() != null) {
            if (BuildVersionUtil.isOreoOrHigher()) {
                QiscusNetworkCheckerJobService.scheduleJob(getApps());
            }
        }else{
            EventBus.getDefault().post((QiscusInitWithCustomServerEvent.notSetup));
        }
    }

    /**
     * Use this method if we need application context instance
     *
     * @return Your application instance
     */
    public static Application getApps() {
        checkAppIdSetup();
        return appComponent.getApplication();
    }

    /**
     * AppId checker
     *
     * @throws RuntimeException
     */
    public static void checkAppIdSetup() throws RuntimeException {
        if (appComponent.getAppServer() == null) {
            throw new RuntimeException("Please init Qiscus with your app id before!");
        }
    }

    /**
     * Accessor to get current qiscus app id
     *
     * @return Current app id
     */
    public static String getAppId() {
        checkAppIdSetup();
        return appComponent.getAppId();
    }

    /**
     * Accessor to get current VersionSDK
     *
     * @return Current VersionSDK
     */
    public static Boolean getIsBuiltIn() {
        return appComponent.getIsBuiltIn();
    }

    /**
     * Accessor to get current qiscus app server
     *
     * @return Current qiscus app server
     */
    public static String getAppServer() {
        checkAppIdSetup();
        return appComponent.getAppServer();
    }

    /**
     * isEnableMqttLB
     * Checker for enable or disable own MQTT Load Balancer
     *
     * @return boolean
     */
    public static boolean isEnableMqttLB() {
        checkAppIdSetup();
        return appComponent.getEnableMqttLB();
    }

    /**
     * enableEventReport
     * Checker for enable or disable EventReport
     *
     * @return boolean
     */
    public static boolean getEnableEventReport() {
        return appComponent.getEnableEventReport();
    }

    /**
     * enableRealtime
     * Checker for enable or disable Realtime
     *
     * @return boolean
     */
    public static boolean getEnableRealtime() {
        return appComponent.getEnableRealtime();
    }

    public static void setEnableDisableRealtime(Boolean enableDisableRealtime){
        appComponent.setEnableRealtime(enableDisableRealtime);
    }

    /**
     * enableSync
     * Checker for enable or disable sync
     *
     * @return boolean
     */
    public static boolean getEnableSync() {
        return appComponent.getEnableSync();
    }

    /**
     * enableSync
     * Checker for enable or disable sync
     *
     * @return boolean
     */
    public static boolean getEnableSyncEvent() {
        return appComponent.getEnableSyncEvent();
    }

    /**
     * syncServiceDisabled
     * Checker for know if we force stop the sync service
     *
     * @return boolean
     */

    public static Boolean isSyncServiceDisabledManually() {
        return appComponent.getSyncServiceDisabled();
    }


    /**
     * autoRefreshToken
     *
     * @return boolean
     */

    public static Boolean isAutoRefreshToken() {
        return appComponent.getAutoRefreshToken();
    }

    /**
     * openRealtimeConnection
     * Open realtime connection (manual)
     *
     * @return boolean
     */
    public static Boolean openRealtimeConnection(){
        if (QiscusCore.hasSetupUser() && QiscusAndroidUtil.isNetworkAvailable() && QiscusCore.getEnableRealtime()) {
            getLocalDataManager().setEnableDisableRealtimeManually(true);
            QiscusPusherApi.getInstance().restartConnection();
            return true;
        } else {
            return false;
        }
    }

    /**
     * closeRealtimeConnection
     * Close realtime connection (manual)
     *
     * @return boolean
     */
    public static Boolean closeRealtimeConnection(){
        if (QiscusCore.hasSetupUser()) {
            QiscusPusherApi.getInstance().disconnect();
            getLocalDataManager().setEnableDisableRealtimeManually(false);
            return true;
        } else {
            return false;
        }

    }

    public static Boolean getStatusRealtimeEnableDisable(){
        return getLocalDataManager().getEnableDisableRealtimeManually();
    }

    /**
     * Accessor to get current mqtt broker url
     *
     * @return Current mqtt broker url
     */
    public static String getMqttBrokerUrl() {
        checkAppIdSetup();
        appComponent.getLocalDataManager()
                .setMqttBrokerUrl(appComponent.getMqttBrokerUrl());

        return isEnableMqttLB() ?  appComponent.getLocalDataManager()
                .getMqttBrokerUrl() : appComponent.getMqttBrokerUrl();
    }

    /**
     * Accessor to get current mqtt broker url
     *
     * @return Current mqtt broker url
     */
    public static String getBaseURLLB() {
        checkAppIdSetup();

        if ( appComponent.getLocalDataManager()
                .getURLLB() == null) {
            appComponent.getLocalDataManager()
                    .setURLLB(appComponent.getBaseURLLB());
        }

        return isEnableMqttLB() ?  appComponent.getLocalDataManager()
                .getURLLB() : appComponent.getBaseURLLB();
    }

    /**
     * this method is used if isEnableMqttLB() == true
     *
     * @param mqttBaseUrl
     */
    public static void setCacheMqttBrokerUrl(String mqttBaseUrl, boolean everConnected) {
        appComponent.getLocalDataManager()
                .setMqttBrokerUrl(mqttBaseUrl);
        appComponent.getLocalDataManager()
                .setWillGetNewNodeMqttBrokerUrl(everConnected);

    }

    /**
     * this is mechanism used by MQTT LB
     *
     * @return boolean
     */
    public static boolean willGetNewNodeMqttBrokerUrl() {
        return  appComponent.getLocalDataManager()
                .willGetNewNodeMqttBrokerUrl();
    }

    public static void setWillGetNewNodeMqttBrokerUrl(Boolean set) {
        appComponent.getLocalDataManager()
                .setWillGetNewNodeMqttBrokerUrl(set);
    }


    /**
     * Accessor to get current LocalDataManager
     *
     * @return current localDataManager
     */
    @Deprecated
    public static LocalDataManager getLocalDataManager() {
        checkAppIdSetup();
        return appComponent.getLocalDataManager();
    }

    /**
     * For checking is qiscus user has been setup
     *
     * @return true if already setup, false if not yet
     */
    public static boolean hasSetupUser() {
        return  appComponent.getAppServer() != null
                &&  appComponent.getLocalDataManager().isLogged();

    }

    /**
     * For checking is client has been setupAppID
     *
     * @return true if already hasSetupAppID, false if not yet
     */
    public static boolean hasSetupAppID() {
        return  appComponent.getAppServer() != null;
    }

    /**
     * Accessor to get current qiscus user account
     *
     * @return Current qiscus user account
     */
    public static QiscusAccount getQiscusAccount() {
        checkUserSetup();
        return  appComponent.getLocalDataManager()
                .getAccountInfo();
    }

    /**
     * User Checker
     *
     * @throws RuntimeException
     */
    public static void checkUserSetup() throws RuntimeException {
        checkAppIdSetup();
        if (!hasSetupUser()) {
            throw new RuntimeException("Please set Qiscus user before start the chatting!");
        }
    }

    /**
     * Accessor to get current qiscus user token
     *
     * @return Current qiscus user token
     */
    public static String getToken() {
        checkUserSetup();
        return  appComponent.getLocalDataManager()
                .getToken();
    }

    /**
     * Accessor to get current qiscus refresh token
     *
     * @return Current qiscus user refresh token
     */
    public static String getRefreshToken() {
        checkUserSetup();
        return  appComponent.getLocalDataManager()
                .getRefreshToken();
    }

    /**
     * Use this method to set new qiscus refresh token
     *
     * @param newToken the QiscusRefreshToken
     */
    public static void saveRefreshToken(QiscusRefreshToken newToken) {
        checkUserSetup();
        QiscusAccount account =  appComponent.getLocalDataManager()
                .getAccountInfo();
        account.setToken(newToken.getToken());
        account.setRefreshToken(newToken.getRefreshToken());
        account.setTokenExpiresAt(newToken.getTokenExpiresAt());
        appComponent.getLocalDataManager()
                .saveAccountInfo(account);
    }

    /**
     * Get the current qiscus heartbeat duration
     *
     * @return Heartbeat duration in milliseconds
     */
    public static long getHeartBeat() {
        return  appComponent.getHeartBeat();
    }

    /**
     * Set the heartbeat of qiscus synchronization chat data. Default value is 500ms
     *
     * @param heartBeat Heartbeat duration in milliseconds
     */
    @Deprecated
    public static void setHeartBeat(long heartBeat) {
        checkAppIdSetup();
        appComponent.setHeartBeat((int) heartBeat);

    }

    /**
     * Get the current qiscus automaticheartbeat duration (default 30s)
     *
     * @return automaticHeartbeat duration in milliseconds
     */
    public static long getAutomaticHeartBeat() {
        return  appComponent.getAutomaticHeartBeat();
    }

    public static long getNetworkConnectionInterval(){
        return  appComponent.getNetworkConnectionInterval();
    }

    /**
     * Set the syncInterval of qiscus synchronization chat data. Default value is 500ms
     *
     * @param interval Heartbeat duration in milliseconds
     */
    public static void setSyncInterval(long interval) {
        checkAppIdSetup();
        appComponent.setHeartBeat((int) interval);
    }

    /**
     * Get current qiscus chat data store
     *
     * @return Singleton of qiscus data store
     */
    public static QiscusDataStore getDataStore() {
        return  appComponent.getDataStore();
    }

    /**
     * Use this method if you want to use your own data store implementation, e.g using Realm, your own table,
     * your own orm, etc
     *
     * @param dataStore Your own chat datastore
     */
    public static void setDataStore(QiscusDataStore dataStore) {
        //TODO ARIEF
        //QiscusCore.dataStore = dataStore;
        appComponent.setDataStore(dataStore);
    }

    /**
     * Use this method to get current qiscus chatting configuration, you can also modify it.
     *
     * @return Current qiscus chatting configuration
     */
    public static QiscusCoreChatConfig getChatConfig() {
        return config;
    }

    /**
     * Use this method set qiscus user. If user doesn't exist at your qiscus engine, He/She will be
     * registered automatically.
     *
     * @param userEmail The email or username of qiscus user
     * @param userKey   Qiscus user key
     * @return User builder
     */
    public static SetUserBuilder setUser(String userEmail, String userKey) {
        return new SetUserBuilder(userEmail, userKey);
    }

    /**
     * Use this method to set qiscus user with jwt token from your apps backend
     *
     * @param token the jwt token
     * @return observable of qiscus account
     */
    @Deprecated
    public static Observable<QiscusAccount> setUserAsObservable(String token) {
        return QiscusApi.getInstance()
                .login(token)
                .doOnNext(qiscusAccount -> {
                    if (QiscusCore.hasSetupUser()) {
                        appComponent.getLocalDataManager().saveAccountInfo(qiscusAccount);
                        configureFcmToken();
                    } else {
                        appComponent.getLocalDataManager().saveAccountInfo(qiscusAccount);
                        configureFcmToken();
                        EventBus.getDefault().post(QiscusUserEvent.LOGIN);
                    }
                });
    }

    /**
     * Use this method to set qiscus user with jwt token from your apps backend
     *
     * @param token the jwt token
     * @return observable of qiscus account
     */
    public static Observable<QiscusAccount> setUserWithIdentityToken(String token) {
        return QiscusApi.getInstance()
                .setUserWithIdentityToken(token)
                .doOnNext(qiscusAccount -> {
                    if (QiscusCore.hasSetupUser()) {
                        appComponent.getLocalDataManager()
                                .saveAccountInfo(qiscusAccount);

                        configureFcmToken();
                    } else {
                        appComponent.getLocalDataManager()
                                .saveAccountInfo(qiscusAccount);
                        configureFcmToken();
                        EventBus.getDefault().post(QiscusUserEvent.LOGIN);
                    }
                });
    }

    /**
     * Use this method to set qiscus user with jwt token from your apps backend
     *
     * @param token    the jwt token
     * @param listener completion listener
     */
    @Deprecated
    public static void setUser(String token, SetUserListener listener) {
        setUserWithIdentityToken(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener::onSuccess, listener::onError);
    }

    /**
     * Use this method to set qiscus user with jwt token from your apps backend
     *
     * @param token    the jwt token
     * @param listener completion listener
     */
    public static void setUserWithIdentityToken(String token, SetUserListener listener) {
        setUserWithIdentityToken(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener::onSuccess, listener::onError);
    }

    /**
     * Use this method to update qiscus user data such as name and avatar
     *
     * @param name      user name
     * @param avatarUrl user avatar url
     * @param extras    user extras
     * @return observable of qiscus account
     */
    public static Observable<QiscusAccount> updateUserAsObservable(String name, String avatarUrl, JSONObject extras) {
        return QiscusApi.getInstance().updateUser(name, avatarUrl, extras)
                .doOnNext(qiscusAccount -> {

                    if (qiscusAccount.getToken().isEmpty()) {
                        qiscusAccount.setToken(getQiscusAccount().getToken());
                    }
                    appComponent.getLocalDataManager()
                            .saveAccountInfo(qiscusAccount);
                });

    }

    /**
     * Use this method to update qiscus user data such as name and avatar
     *
     * @param name      user name
     * @param avatarURL user avatar url
     * @param extras    user extras
     * @return observable of qiscus account
     */
    public static Observable<QiscusAccount> updateUser(String name, String avatarURL, JSONObject extras) {
        return QiscusApi.getInstance().updateUser(name, avatarURL, extras)
                .doOnNext(qiscusAccount -> {
                    if (qiscusAccount.getToken().isEmpty()) {
                        qiscusAccount.setToken(getQiscusAccount().getToken());
                    }
                    appComponent.getLocalDataManager()
                            .saveAccountInfo(qiscusAccount);
                });
    }

    /**
     * Use this method to update qiscus user data such as name and avatar
     *
     * @param name      user name
     * @param avatarUrl user avatar url
     * @return observable of qiscus account
     */
    public static Observable<QiscusAccount> updateUserAsObservable(String name, String avatarUrl) {
        return updateUserAsObservable(name, avatarUrl, null);
    }

    /**
     * Use this method to update qiscus user data such as name and avatar
     *
     * @param name      user name
     * @param avatarUrl user avatar url
     * @param extras    user extras
     * @param listener  completion listener
     */
    public static void updateUser(String name, String avatarUrl, JSONObject extras, SetUserListener listener) {
        checkUserSetup();
        updateUser(name, avatarUrl, extras)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener::onSuccess, listener::onError);
    }

    /**
     * Use this method to update qiscus user data such as name and avatar
     *
     * @param name      user name
     * @param avatarUrl user avatar url
     * @param listener  completion listener
     */
    public static void updateUser(String name, String avatarUrl, SetUserListener listener) {
        updateUser(name, avatarUrl, null, listener);
    }

    /**
     * Will return the exact apps name
     *
     * @return The apps name.
     */
    public static String getAppsName() {
        checkAppIdSetup();
        return appComponent.getApplication()
                .getApplicationInfo().loadLabel(
                        appComponent.getApplication().getPackageManager()
                ).toString();
    }

    /**
     * Needed to run something at main thread handler
     *
     * @return Main thread handler
     */
    public static Handler getAppsHandler() {
        checkAppIdSetup();
        return appComponent.getAppHandler();
    }

    public static JSONObject getCustomHeader() {
        return customHeader;
//        return appComponent.getCustomHeader();
    }

    /**
     * Use this method to set custom header
     *
     * @param customHeader custom header
     */
    public static void setCustomHeader(JSONObject customHeader) {
        QiscusCore.customHeader = customHeader;
        //appComponent.setCustomHeader(customHeader);
    }

    /**
     * @return current fcm token, null if not set
     */
    public static String getFcmToken() {
        return appComponent.getLocalDataManager().getFcmToken();
    }

    /**
     * Set the FCM token to configure push notification with firebase cloud messaging
     *
     * @param fcmToken the token
     */
    @Deprecated
    public static void setFcmToken(String fcmToken) {
        if (hasSetupUser() && getChatConfig().isEnableFcmPushNotification()) {
            QiscusApi.getInstance().registerFcmToken(fcmToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aVoid -> {
                    }, throwable -> QiscusErrorLogger.print("SetFCMToken", throwable));
        }

        appComponent.getLocalDataManager()
                .setFcmToken(fcmToken);
    }

    /**
     * Set the FCM token to configure push notification with firebase cloud messaging
     *
     * @param token the token (fcmToken)
     */
    public static void registerDeviceToken(String token) {
        if (hasSetupUser() && getChatConfig().isEnableFcmPushNotification()) {
            QiscusApi.getInstance().registerDeviceToken(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aVoid -> {
                    }, throwable -> QiscusErrorLogger.print("SetFCMToken", throwable));
        }

        appComponent.getLocalDataManager()
                .setFcmToken(token);
    }

    /**
     * Set the FCM token to configure push notification with firebase cloud messaging
     *
     * @param token the token (fcmToken) & packageId
     */
    public static void registerDeviceToken(String token, String packageId) {
        if (hasSetupUser() && getChatConfig().isEnableFcmPushNotification()) {
            QiscusApi.getInstance().registerDeviceToken(token, packageId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aVoid -> {
                    }, throwable -> QiscusErrorLogger.print("SetFCMToken", throwable));
        }

        appComponent.getLocalDataManager()
                .setFcmToken(token);
    }

    /**
     * Remove the FCM token
     *
     * @param token the token (fcmToken)
     */
    public static void removeDeviceToken(String token) {
        if (hasSetupUser()) {
            QiscusApi.getInstance().removeDeviceToken(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aVoid -> {
                    }, throwable -> QiscusErrorLogger.print("SetFCMToken", throwable));
        }

        appComponent.getLocalDataManager()
                .setFcmToken(null);
    }

    private static void configureFcmToken() {
        if (hasSetupUser() && getChatConfig().isEnableFcmPushNotification()) {
            String fcmToken = getFcmToken();
            if (fcmToken != null) {
                registerDeviceToken(fcmToken);
            } else {
                try {
                    FirebaseMessaging.getInstance().deleteToken();
                } catch ( IllegalStateException e) {
                    //Do nothing
                }
            }
        }
    }

    /**
     * Check is apps opened on foreground
     *
     * @return true if apps on foreground, and false if on background
     */
    public static boolean isOnForeground() {
      //  return QiscusActivityCallback.INSTANCE.isForeground();
        return BackgroundForegroundListener.INSTANCE.isForeground();
    }

    public static void setAppInForeground(){
        //QiscusActivityCallback.INSTANCE.setAppActiveOrForground();
        BackgroundForegroundListener.INSTANCE.setAppActiveOrForground();
    }

    /**
     * Needed to run something at background thread handler
     *
     * @return ScheduledExecutorService instance
     */
    public static ScheduledThreadPoolExecutor getTaskExecutor() {
        checkAppIdSetup();
        return appComponent.getTaskExecutor();
    }

    /**
     * all current qiscus refresh token, You can call this method when you get Unauthorized event for example.
     */

    public static void refreshToken(SetRefreshTokenListener listener) {
        if (hasSetupUser()) {
            QiscusAccount account = appComponent.getLocalDataManager()
                    .getAccountInfo();
            QiscusApi.getInstance().refreshToken(account.getEmail(), account.getRefreshToken())
                    .doOnNext(QiscusCore::saveRefreshToken)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(refreshToken -> {
                        if (listener != null) listener.onSuccess(refreshToken);
                    }, throwable -> {
                        if (listener != null) listener.onError(throwable);
                    });
        }
    }

    private static void autoRefreshToken() {
        if (isValidToRefreshToken(
                appComponent.getLocalDataManager()
                        .getAccountInfo())
        ) {
            QiscusAndroidUtil.runOnBackgroundThread(() ->
                    refreshToken(new SetRefreshTokenListener() {
                        @Override
                        public void onSuccess(QiscusRefreshToken refreshToken) {
                            QiscusLogger.print(
                                    "AutoRefreshToken", refreshToken != null ? "success" : "failed"
                            );
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            QiscusErrorLogger.print(throwable);
                            //need to relogin
                            EventBus.getDefault().post(
                                    new QiscusRefreshTokenEvent(
                                            401, "Unauthorized"
                                    )
                            );
                        }
                    })
            );
        }
    }

    private static boolean isValidToRefreshToken(QiscusAccount account) {
        if (account.getTokenExpiresAt().isEmpty()) {
            return false;
        }else{
            Date expiredAt = QiscusDateUtil.getDateTimeSdf(account.getTokenExpiresAt());
            return DateUtils.isToday(expiredAt.getTime())
                    || QiscusDateUtil.isBeforeADaySdf(expiredAt.getTime())
                    || QiscusDateUtil.isPassingDateTimeSdf(expiredAt.getTime());
        }
    }

    public static void clearUser() {
        if (hasSetupUser()) {
            if (appComponent.getEnableRefreshToken()) {
                QiscusAccount account = appComponent.getLocalDataManager()
                        .getAccountInfo();
                QiscusApi.getInstance().logout(account.getEmail(), account.getToken())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            clearData();
                        }, throwable -> {
                            clearData();
                        });
            } else {
                clearData();
            }
        }else{
            clearData();
        }
    }

    public static void clearData(){
        if (BuildVersionUtil.isOreoOrHigher()) {
            JobScheduler jobScheduler = (JobScheduler) appComponent.getApplication()
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }
        }
        appComponent.getLocalDataManager().clearData();
        appComponent.getDataStore().clear();

        QiscusCacheManager.getInstance().clearData();
        EventBus.getDefault().post(QiscusUserEvent.LOGOUT);
    }

    /**
     * Set the custom key for the database.
     * The key length must be at least 8 characters.
     *
     * @param key The custom key for the database
     */
    public static void setCustomKey(String key) {
        appComponent.setCustomKey(key);
    }

    public interface SetUserListener {
        /**
         * Called if saving user succeed
         *
         * @param qiscusAccount Saved qiscus account
         */
        void onSuccess(QiscusAccount qiscusAccount);

        /**
         * Called if error happened while saving qiscus user account. e.g network error
         *
         * @param throwable The cause of error
         */
        void onError(Throwable throwable);
    }

    public interface SetRefreshTokenListener {
        /**
         * Called if refresh token succeed
         *
         * @param refreshToken Saved qiscus refresh token
         */
        void onSuccess(QiscusRefreshToken refreshToken);

        /**
         * Called if error happened while saving qiscus refresh token. e.g network error
         *
         * @param throwable The cause of error
         */
        void onError(Throwable throwable);
    }

    public interface LogoutListener {
        /**
         * Called if logout succeed
         *
         */
        void onSuccess();

        /**
         * Called if error happened while saving qiscus logout. e.g network error
         *
         * @param throwable The cause of error
         */
        void onError(Throwable throwable);
    }

    public static class LocalDataManager {
        private SharedPreferences sharedPreferences;
        private final Gson gson;
        private String token;
        private String refreshToken;

        public LocalDataManager(Application application) {
            SharedPreferences sharedPreferencesOld  = application.getSharedPreferences("qiscus.cfg", Context.MODE_PRIVATE);

            try {
                String sharedPrefsFile = JupukData.getFileName();
                boolean isActive = false;

                if (isActive) {
                    sharedPreferences = EncryptedSharedPreferences.create(
                            sharedPrefsFile,
                            JupukData.getFileKey(),
                            application.getApplicationContext(),
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

                } else {
                    MasterKey masterKey = new MasterKey.Builder(application.getApplicationContext(), JupukData.getFileKey())
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    sharedPreferences = EncryptedSharedPreferences.create(application.getApplicationContext(),
                            sharedPrefsFile,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                }

                String dataAccount = sharedPreferencesOld.getString("cached_account", "");
                String LbUrl = sharedPreferencesOld.getString("lb_url", "");
                String brokerUrl = sharedPreferencesOld.getString("mqtt_broker_url", "");
                String fcmToken = sharedPreferencesOld.getString("fcm_token", "");


                SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();

                if (!dataAccount.isEmpty()) {
                    //migration to new Data
                    sharedPrefsEditor.putString("cached_account", dataAccount);
                }

                if (!LbUrl.isEmpty()) {
                    sharedPrefsEditor.putString("lb_url", LbUrl);
                }

                if (!brokerUrl.isEmpty()) {
                    sharedPrefsEditor.putString("mqtt_broker_url", brokerUrl);
                }

                if (!fcmToken.isEmpty()) {
                    sharedPrefsEditor.putString("fcm_token", fcmToken);
                }

                sharedPrefsEditor.apply();

                //remove old data
                sharedPreferencesOld.edit().clear().apply();


            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            } catch (IOException e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            } catch (SecurityException e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            } catch (Exception e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            } catch (Error e) {
                e.printStackTrace();
                sharedPreferences = sharedPreferencesOld;
            }

            gson = new Gson();
            token = isLogged() ? getAccountInfo().getToken() : "";
            refreshToken = isLogged() ? getAccountInfo().getRefreshToken() : "";
        }

        private boolean isLogged() {
            return sharedPreferences.contains("cached_account");
        }

        private void saveAccountInfo(QiscusAccount qiscusAccount) {
            try {
                JSONObject data = new JSONObject(qiscusAccount.toString().substring(13));
                sharedPreferences.edit().putString("cached_account", data.toString()).apply();
            } catch (JSONException e) {
                sharedPreferences.edit().putString("cached_account", gson.toJson(qiscusAccount)).apply();
                e.printStackTrace();
            }

            setToken(qiscusAccount.getToken());
            setRefreshToken(qiscusAccount.getRefreshToken());
        }

        private QiscusAccount getAccountInfo() {
            QiscusAccount qiscusAccount = new QiscusAccount();
            try {
                JSONObject jsonObject = new JSONObject(sharedPreferences.getString("cached_account", ""));
                if (jsonObject.has("avatar")) {
                    qiscusAccount.setAvatar(jsonObject.optString("avatar", ""));
                }
                if (jsonObject.has("email")) {
                    qiscusAccount.setEmail(jsonObject.optString("email", ""));
                }
                if (jsonObject.has("id")) {
                    qiscusAccount.setId(jsonObject.optInt("id", 0));
                }
                if (jsonObject.has("token")) {
                    qiscusAccount.setToken(jsonObject.optString("token", ""));
                }
                if (jsonObject.has("refresh_token")) {
                    qiscusAccount.setRefreshToken(jsonObject.optString("refresh_token", ""));
                }
                if (jsonObject.has("token_expires_at")) {
                    qiscusAccount.setTokenExpiresAt(jsonObject.optString("token_expires_at", ""));
                }
                if (jsonObject.has("username")) {
                    qiscusAccount.setUsername(jsonObject.optString("username", ""));
                }

                if (jsonObject.has("extras")) {
                    if (jsonObject.optJSONObject("extras").toString().contains("nameValuePairs")) {
                        //migration from latest
                        qiscusAccount.setExtras(jsonObject.optJSONObject("extras").getJSONObject("nameValuePairs"));
                    } else {
                        qiscusAccount.setExtras(jsonObject.optJSONObject("extras"));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return qiscusAccount;
        }

        private String getToken() {
            return token == null ? token = "" : token;
        }

        private void setToken(String token) {
            this.token = token;
        }

        private String getRefreshToken() {
            return refreshToken == null ? refreshToken = "" : refreshToken;
        }

        private void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        private String getFcmToken() {
            return sharedPreferences.getString("fcm_token", null);
        }

        private void setFcmToken(String fcmToken) {
            sharedPreferences.edit().putString("fcm_token", fcmToken).apply();
        }

        /**
         * this is used if enableMqttLB = true
         *
         * @return mqttBrokerUrl
         */
        private String getMqttBrokerUrl() {
            return sharedPreferences.getString("mqtt_broker_url", null);
        }

        private void setEnableDisableRealtimeManually(Boolean enableDisableRealtimeManually) {
            sharedPreferences.edit().putBoolean("realtime_enable_disable", enableDisableRealtimeManually).apply();
        }

        /**
         * save local sharedPref for enable / disable realtime (manual)
         *
         * @return mqttBrokerUrl
         */
        private Boolean getEnableDisableRealtimeManually() {
            return sharedPreferences.getBoolean("realtime_enable_disable", true);
        }


        /**
         * this is used if enableMqttLB = true
         *
         * @param mqttBrokerUrl
         */
        private void setMqttBrokerUrl(String mqttBrokerUrl) {
            sharedPreferences.edit().putString("mqtt_broker_url", mqttBrokerUrl).apply();
        }

        /**
         * this is used if enableMqttLB = true
         *
         * @return UrlLB
         */
        private String getURLLB() {
            return sharedPreferences.getString("lb_url", null);
        }

        /**
         * this is used if enableMqttLB = true
         *
         * @param urlLb
         */
        private void setURLLB(String urlLb) {
            sharedPreferences.edit().putString("lb_url", urlLb).apply();
        }

        /**
         * Mechanism for MQTT LB
         *
         * @return boolean
         */
        private boolean willGetNewNodeMqttBrokerUrl() {
            return sharedPreferences.getBoolean("mqtt_will_get_new", true);
        }

        private void setWillGetNewNodeMqttBrokerUrl(boolean will) {
            sharedPreferences.edit().putBoolean("mqtt_will_get_new", will).apply();
        }

        private void clearData() {
            sharedPreferences.edit().clear().apply();
            setToken("");
            setRefreshToken("");
        }
    }

    public static class SetUserBuilder {
        private String email;
        private String password;
        private String username;
        private String avatarUrl;
        private JSONObject extras;

        private SetUserBuilder(String email, String password) {
            this.email = email;
            this.password = password;
            this.username = email;
        }

        /**
         * Set the qiscus user name, if you not call this method, the username will be the same with
         * qiscus user email
         *
         * @param username The name
         * @return builder
         */
        public SetUserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the avatar url for the qiscus user.
         *
         * @param avatarUrl The string url of avatar
         * @return builder
         */
        public SetUserBuilder withAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        /**
         * Set user extras to qiscus
         *
         * @param extras JSONObject
         * @return builder
         */
        public SetUserBuilder withExtras(JSONObject extras) {
            this.extras = extras;
            return this;
        }

        /**
         * Submit to qiscus engine and save the user account
         *
         * @param listener Listener of saving user process
         */
        public void save(SetUserListener listener) {
            save().subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(listener::onSuccess, listener::onError);
        }

        /**
         * Submit to qiscus engine and save the user account with RxJava style.
         *
         * @return Observable of Qiscus account
         */
        public Observable<QiscusAccount> save() {
            return QiscusApi.getInstance()
                    .setUser(email, password, username, avatarUrl, extras)
                    .doOnNext(qiscusAccount -> {
                        if (QiscusCore.hasSetupUser()) {
                            appComponent.getLocalDataManager()
                                    .saveAccountInfo(qiscusAccount);

                            configureFcmToken();
                        } else {
                            appComponent.getLocalDataManager()
                                    .saveAccountInfo(qiscusAccount);
                            configureFcmToken();
                            EventBus.getDefault().post(QiscusUserEvent.LOGIN);
                        }
                    });
        }
    }
}
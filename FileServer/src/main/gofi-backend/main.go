package main

import "C"
import (
    "github.com/aviddiviner/gin-limit"
    "github.com/gin-gonic/gin"
    "github.com/sirupsen/logrus"
    "gofi/controller"
    "gofi/db"
    "gofi/extension"
    "gofi/middleware"
    "net/http"
    "os"
    "context"
    "unsafe"
    "reflect"
    "unicode/utf16"
    "gofi/env"
    "path/filepath"
)
/*\
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdlib.h>

const static jstring JString(JNIEnv *env, const char* str) {
    return (*env)->NewStringUTF(env, str);
}
static jsize jni_GetStringLength(JNIEnv *env, jstring str) {
    return (*env)->GetStringLength(env, str);
}
static const jchar *jni_GetStringChars(JNIEnv *env, jstring str) {
    return (*env)->GetStringChars(env, str, NULL);
}
*/
import "C"

func init() {
    extension.BindAdditionalType()
}

var gWebServer *http.Server

//export Java_com_patchself_goserver_FileServer_startServer
func Java_com_patchself_goserver_FileServer_startServer(cenv *C.JNIEnv, ctx C.jobject, webResPath C.jstring, listenJ C.jstring) {
    listen := GoString(cenv, listenJ)
    webStaticPath := GoString(cenv, webResPath)
    logrus.Infof("Gofi is running on %v，current environment is %s,version is %s\n", listen, env.Current(), db.ObtainConfiguration().Version)

    app := gin.Default()

    // 预览模式下,限制请求频率
    if env.IsPreview() {
        app.Use(limit.MaxAllowed(100))
    } else if env.IsProduct() {
        gin.SetMode(gin.ReleaseMode)
    } else if env.IsDevelop() {
        gin.SetMode(gin.DebugMode)
    }

    app.Use(middleware.CORS)
    app.Use(middleware.Language)

    app.Static("/assets", filepath.Join(webStaticPath, "/assets"))
    app.NoRoute(func(context *gin.Context) {
        indexBytes, err := os.ReadFile(filepath.Join(webStaticPath, "/index.html"))
        if err != nil {
            logrus.Fatal(err)
        }
        context.Writer.Header().Set("Content-Type", "text/html; charset=utf-8")
        context.String(http.StatusOK, string(indexBytes))
    })

    api := app.Group("/api")
    {
        api.GET("/configuration", controller.GetConfiguration)
        api.POST("/configuration", middleware.AuthChecker, controller.UpdateConfiguration)
        api.POST("/setup", controller.Setup)
        api.GET("/files", controller.ListFiles)
        api.GET("/file", controller.FileDetail)
        api.GET("/download", controller.Download)
        api.HEAD("download", controller.Download)
        api.POST("/upload", controller.Upload)
    }

    user := api.Group("/user")
    {
        user.GET("", middleware.AuthChecker, controller.GetUser)
        user.POST("/login", controller.Login)
        user.POST("/logout", middleware.AuthChecker, controller.Logout)
        user.POST("/changePassword", middleware.AuthChecker, controller.ChangePassword)
    }

    permission := api.Group("/permission", middleware.AdminChecker)
    {
        permission.GET("/guest", controller.GetGuestPermissions)
        permission.POST("/guest", controller.UpdateGuestPermission)
    }
    gWebServer := &http.Server{Addr: listen, Handler: app}
    _ = gWebServer.ListenAndServe()
}

//export Java_com_patchself_goserver_FileServer_stopServer
func Java_com_patchself_goserver_FileServer_stopServer() {
    if gWebServer != nil {
        gWebServer.Shutdown(context.Background())
        gWebServer = nil
    }
}
func JavaString(env *C.JNIEnv, str string) C.jstring {
    cstr := C.CString(str)
    defer C.free(unsafe.Pointer(cstr))
    return C.JString(env, cstr)
}

// JavaString converts the string to a JVM jstring.
/*func JavaString(env *C.JNIEnv, str string) C.jstring {
    if str == "" {
        return 0
    }
    utf16Chars := utf16.Encode([]rune(str))
    res := C.jni_NewString(env, (*C.jchar)(unsafe.Pointer(&utf16Chars[0])), C.int(len(utf16Chars)))
    return C.jstring(res)
}*/

// GoString converts the JVM jstring to a Go string.
func GoString(env *C.JNIEnv, str C.jstring) string {
    if str == 0 {
        return ""
    }
    strlen := C.jni_GetStringLength(env, str)
    chars := C.jni_GetStringChars(env, str)
    var utf16Chars []uint16
    hdr := (*reflect.SliceHeader)(unsafe.Pointer(&utf16Chars))
    hdr.Data = uintptr(unsafe.Pointer(chars))
    hdr.Cap = int(strlen)
    hdr.Len = int(strlen)
    utf8 := utf16.Decode(utf16Chars)
    return string(utf8)
}

func main() {}

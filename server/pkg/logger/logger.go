package logger

import (
	"io"
	"os"

	log "github.com/sirupsen/logrus"
)

func InitLogger() *log.Logger {
	logger := log.New()

	// 设置日志级别
	logger.SetLevel(log.DebugLevel)

	// 设置日志格式为JSON格式
	logger.SetFormatter(&log.JSONFormatter{})

	// 添加日志输出到文件
	logFile, err := os.OpenFile("app.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err == nil {
		logger.Out = logFile
	} else {
		logger.Info("Failed to log to file, using default stderr")
	}

	// 将日志同时输出到控制台
	logger.SetOutput(io.MultiWriter(os.Stdout, logFile))

	// 将日志按照不同级别写入不同的文件
	// logger.Hooks.Add(lfshook.NewHook(
	// 	lfshook.PathMap{
	// 		log.DebugLevel: "debug.log",
	// 		log.InfoLevel:  "info.log",
	// 		log.WarnLevel:  "warning.log",
	// 		log.ErrorLevel: "error.log",
	// 		log.FatalLevel: "fatal.log",
	// 		log.PanicLevel: "panic.log",
	// 	},
	// 	&log.JSONFormatter{},
	// ))

	return logger
}

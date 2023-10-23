package main

import (
	"fmt"
	"net/http"

	"reverse/internal/proxy"
	"reverse/pkg/logger"
)

func main() {
	logger := logger.InitLogger()
	proxer := proxy.NewReverseProxy(logger)
	fmt.Printf("Starting server at port 8088\n")
	if err := http.ListenAndServe(":8088", http.HandlerFunc(proxer.ReverseProxyHandler)); err != nil {
		logger.Fatal(err)
	}
}

package proxy

import (
	"bytes"
	"io"
	"io/ioutil"
	"net/http"
	"net/http/httputil"
	"os"
	"path"
	"reverse/pkg/util"
	"strings"
	"sync"

	log "github.com/sirupsen/logrus"
)

type Proxier struct {
	// Handler func(w http.ResponseWriter, r *http.Request)
	logger *log.Logger
	proxy  *httputil.ReverseProxy
}

func NewReverseProxy(log *log.Logger) *Proxier {

	return &Proxier{logger: log}
}

func (proxy *Proxier) ReverseProxyHandler(w http.ResponseWriter, r *http.Request) {
	proxy.logger.Info("[Receive a request from]: ", r.RemoteAddr)

	proxy.logger.Info("[Requset path]: ", r.URL.Path)

	// 获取当前工作目录
	currentDir, err := os.Getwd()
	if err != nil {
		proxy.logger.Error("[Get current err]: ", err)
		return
	}
	filePath := path.Join(currentDir, strings.Replace(r.URL.Path, "/api", "", -1))

	fileDir := util.GetOtherURLParts(filePath)
	fileName := util.GetLastURLPart(filePath)
	var saveLocal bool = false
	if strings.HasSuffix(fileName, ".vector.pbf") || strings.HasSuffix(fileName, ".pbf") {
		saveLocal = true
	} else {
		saveLocal = false
	}
	// Check if the file exists
	if util.FileExists(filePath) {
		proxy.logger.Info("[Get from local]")

		// If the file exists, read its content and return it
		content, err := ioutil.ReadFile(filePath)
		if err != nil {
			http.Error(w, "Error reading file", http.StatusInternalServerError)
			return
		}
		w.Write(content)
	} else {
		proxy.logger.Info("[Get from proxy] ")
		// If the file does not exist, proxy the request
		var target string

		if strings.Contains(r.URL.Path, "/api") {
			target = "a.tiles.mapbox.com"
		} else {
			target = "api.mapbox.com"
		}

		director := func(req *http.Request) {
			req.URL.Scheme = "https"
			req.URL.Host = target
			req.Host = target
			if strings.Contains(req.URL.Path, "/api") {
				req.URL.Path = strings.Replace(req.URL.Path, "/api", "", -1)
			}
		}
		modifyResponse := func(res *http.Response) error {

			var wg sync.WaitGroup
			wg.Add(1)
			if res.StatusCode == http.StatusOK && saveLocal {
				// 将响应数据写入文件
				go func() {
					defer wg.Done()
					// 递归创建目录
					err := os.MkdirAll(fileDir, os.ModePerm)
					if err != nil {
						proxy.logger.Error("[Error creating directories]: ", err)
						return
					}
					var responseBodyBuffer bytes.Buffer

					var tempFileName = filePath + ".temp"

					tempFile, err := os.Create(tempFileName)
					if err != nil {
						proxy.logger.Error("[Error creating tempFile]: ", err)
					}

					defer tempFile.Close()

					multiWriter := io.MultiWriter(&responseBodyBuffer, tempFile)

					_, err = io.Copy(multiWriter, res.Body)

					res.Body = ioutil.NopCloser(&responseBodyBuffer)
					// res.Body = ioutil.NopCloser(io.TeeReader(res.Body, multiWriter))

					if err != nil {
						proxy.logger.Error("[Error in write file]: ", err)
					} else {
						err = os.Rename(tempFileName, filePath)
						if err != nil {
							proxy.logger.Error("[Error in rename file]: ", err)
						} else {
							proxy.logger.Info("[Success in write file]", filePath)
						}
					}
				}()

			} else {
				wg.Done()
			}
			wg.Wait() // 等待文件写入完成
			// 可以在此处做其他处理
			return nil
		}
		proxyer := &httputil.ReverseProxy{Director: director, ModifyResponse: modifyResponse}

		proxyer.ServeHTTP(w, r)
	}

}

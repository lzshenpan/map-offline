package util

import (
	"net/url"
	"os"
	"path"
)

// Check if a file exists
func FileExists(filename string) bool {
	_, err := os.Stat(filename)
	return err == nil
}

// 获取路径除了最后一部分的其他部分
func GetOtherURLParts(pathStr string) string {
	// 使用path包获取除了最后一部分之外的路径
	return path.Dir(pathStr)
}

// 获取路径的最后一部分
func GetLastURLPart(pathStr string) string {
	// 使用path包获取路径的最后一部分
	return path.Base(pathStr)
}

func GetFirstLevelPath(rawURL string) (string, error) {
	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		return "", err
	}

	// 获取路径的第一级目录
	_, firstLevelPath := path.Split(parsedURL.Path)

	return firstLevelPath, nil
}

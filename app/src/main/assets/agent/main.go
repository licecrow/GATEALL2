package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strings"
)

type Entry struct {
	Name string `json:"name"`
	Path string `json:"path"`
	Dir  bool   `json:"dir"`
	Size int64  `json:"size"`
	Mime string `json:"mime"`
}

var root string

func safe(p string) string {
	clean := filepath.Clean("/" + p)
	full := filepath.Join(root, clean)
	if !strings.HasPrefix(full, root) {
		return root
	}
	return full
}

func mimeOf(name string) string {
	switch strings.ToLower(filepath.Ext(name)) {
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	case ".webp":
		return "image/webp"
	case ".heic":
		return "image/heic"
	case ".mp4":
		return "video/mp4"
	case ".mkv":
		return "video/x-matroska"
	case ".webm":
		return "video/webm"
	case ".mov":
		return "video/quicktime"
	case ".3gp":
		return "video/3gpp"
	case ".mp3":
		return "audio/mpeg"
	case ".m4a":
		return "audio/mp4"
	case ".pdf":
		return "application/pdf"
	}
	return ""
}

func listHandler(w http.ResponseWriter, r *http.Request) {
	full := safe(r.URL.Query().Get("path"))

	rel := strings.TrimPrefix(full, root)
	if rel == "" {
		rel = "/"
	}

	items, err := os.ReadDir(full)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	out := make([]Entry, 0, len(items))
	for _, it := range items {
		info, err := it.Info()
		if err != nil {
			continue
		}

		p := filepath.Join(rel, it.Name())
		if !strings.HasPrefix(p, "/") {
			p = "/" + p
		}

		out = append(out, Entry{
			Name: it.Name(),
			Path: p,
			Dir:  it.IsDir(),
			Size: info.Size(),
			Mime: mimeOf(it.Name()),
		})
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(out)
}

func fileHandler(w http.ResponseWriter, r *http.Request) {
	full := safe(r.URL.Query().Get("path"))
	http.ServeFile(w, r, full) // Range nativo -> seek de video funciona
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
	host, _ := os.Hostname()

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{
		"agent":   "gateall",
		"version": "1.0",
		"host":    host,
		"brand":   readProp("ro.product.brand"),
		"model":   readProp("ro.product.model"),
		"android": readProp("ro.build.version.release"),
		"arch":    runtime.GOARCH,
	})
}

func readProp(key string) string {
	b, err := os.ReadFile("/system/build.prop")
	if err != nil {
		return ""
	}

	for _, line := range strings.Split(string(b), "\n") {
		if strings.HasPrefix(line, key+"=") {
			return strings.TrimSpace(strings.TrimPrefix(line, key+"="))
		}
	}
	return ""
}

func main() {
	port := flag.Int("port", 8765, "porta de escuta")
	r := flag.String("root", "/sdcard", "raiz exposta")
	flag.Parse()

	root = filepath.Clean(*r)

	mux := http.NewServeMux()
	mux.HandleFunc("/ping", pingHandler)
	mux.HandleFunc("/list", listHandler)
	mux.HandleFunc("/file", fileHandler)

	addr := fmt.Sprintf("0.0.0.0:%d", *port)
	log.Printf("GATEALL agent em %s raiz=%s", addr, root)

	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatal(err)
	}
}

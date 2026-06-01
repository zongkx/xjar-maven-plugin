package main

import (
	"bytes"
	"crypto/md5"
	"crypto/sha1"
	"errors"
	"hash"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)
var jdkmd5 = [][]byte{#{xJar.jdkmd5}}


var xJar = XJar{
	md5:  []byte{#{xJar.md5}},
	sha1: []byte{#{xJar.sha1}},
}

var xKey = XKey{
	algorithm: []byte{#{xKey.algorithm}},
	keysize:   []byte{#{xKey.keysize}},
	ivsize:    []byte{#{xKey.ivsize}},
	password:  []byte{#{xKey.password}},
}

func main() {
	// search the jar to start
	jar, err := JAR(os.Args)
	if err != nil {
		panic(err)
	}

	// parse jar name to absolute path
	path, err := filepath.Abs(jar)
	if err != nil {
		panic(err)
	}

	// verify jar with MD5
	md5, err := MD5(path)
	if err != nil {
		panic(err)
	}
	if bytes.Compare(md5, xJar.md5) != 0 {
		panic(errors.New("invalid jar with MD5"))
	}

	// verify jar with SHA-1
	SHA1, err := SHA1(path)
	if err != nil {
		panic(err)
	}
	if bytes.Compare(SHA1, xJar.sha1) != 0 {
		panic(errors.New("invalid jar with SHA-1"))
	}

	// check agent forbid
	{
		args := os.Args
		l := len(args)
		for i := 0; i < l; i++ {
			arg := args[i]
			if strings.HasPrefix(arg, "-javaagent:") {
				panic(errors.New("agent forbidden"))
			}
		}
	}

	// start java application
	java := os.Args[1]
	if(len(jdkmd5) > 0) {
	    // parse jar name to absolute path
    	path2, err := filepath.Abs(java)
    	if(false == fileExists(path2)) {
	        fname, err2 := exec.LookPath(java)
	        if(err2 == nil) {
	            path2, _ = filepath.Abs(fname)
	        }

    	}
        // verify jar with MD5
        md5, err := MD5(path2)
        if err != nil {
            panic(err)
        }

        pass := 0
        for _, element := range jdkmd5 {
            if bytes.Compare(md5, element) == 0 {
                pass = 1
                break
            }
        }
        if(pass == 0) {
            panic(errors.New("invalid java version， please install correct jdk version!"))
        }


	} else {
	    out, err := exec.Command(java, "--help").Output()
        if err != nil {
            panic(errors.New("invalid java program"))
        }
        jdktest := []string{"HotSpot", "JDK", "Java", "JAVA", "java", "mainclass", "jarfile", "jar",
         "classpath", "cp", "version", "showversion", "agentpath", "jarpath", "agentlib"}
        pass := 0
        for _, element := range jdktest {
            cout := strings.Index(string(out[:]), element)
            if(cout > 0) {
                pass += 1
            }
        }
        if(pass <= 4) {
           panic(errors.New("invalid java program test failed"))
        }
	}



	args := os.Args[2:]
	key := bytes.Join([][]byte{
		xKey.algorithm, {13, 10},
		xKey.keysize, {13, 10},
		xKey.ivsize, {13, 10},
		xKey.password, {13, 10},
	}, []byte{})

	var newarglen = len(args) + 1
	var newargs = make([]string, newarglen)
	newargs[0] = "-XX:+DisableAttachMechanism"
	copy(newargs[1:], args[:])


	cmd := exec.Command(java, newargs...)
	cmd.Stdin = bytes.NewReader(key)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()
	if err != nil {
		panic(err)
	}
}

func fileExists(filename string) bool {
   info, err := os.Stat(filename)
   if os.IsNotExist(err) {
      return false
   }
   return !info.IsDir()
}
// find jar name from args
func JAR(args []string) (string, error) {
	var jar string

	l := len(args)
	for i := 1; i < l-1; i++ {
		arg := args[i]
		if arg == "-jar" {
			jar = args[i+1]
		}
	}

	if jar == "" {
		return "", errors.New("unspecified jar name")
	}

	return jar, nil
}

// calculate file's MD5
func MD5(path string) ([]byte, error) {
	return HASH(path, md5.New())
}

// calculate file's SHA-1
func SHA1(path string) ([]byte, error) {
	return HASH(path, sha1.New())
}

// calculate file's HASH value with specified HASH Algorithm
func HASH(path string, hash hash.Hash) ([]byte, error) {
	file, err := os.Open(path)

	if err != nil {
		return nil, err
	}

	_, _err := io.Copy(hash, file)
	if _err != nil {
		return nil, _err
	}

	sum := hash.Sum(nil)

	return sum, nil
}

type XJar struct {
	md5  []byte
	sha1 []byte
}

type XKey struct {
	algorithm []byte
	keysize   []byte
	ivsize    []byte
	password  []byte
}

import { useEffect, useRef } from 'react'
import { Empty } from 'antd'
import Editor, { loader, type OnMount } from '@monaco-editor/react'
import * as monaco from 'monaco-editor/editor/editor.api'
import editorWorker from 'monaco-editor/editor/editor.worker?worker'
// 按需注册常用语言（仅元数据 + 懒加载 tokenizer），避免把 monaco 全部语言打进包
import 'monaco-editor/languages/definitions/java/register'
import 'monaco-editor/languages/definitions/python/register'
import 'monaco-editor/languages/definitions/javascript/register'
import 'monaco-editor/languages/definitions/typescript/register'
import 'monaco-editor/languages/definitions/go/register'
import 'monaco-editor/languages/definitions/cpp/register' // 注册 c + cpp
import 'monaco-editor/languages/definitions/csharp/register'
import 'monaco-editor/languages/definitions/ruby/register'
import 'monaco-editor/languages/definitions/php/register'
import 'monaco-editor/languages/definitions/kotlin/register'
import 'monaco-editor/languages/definitions/swift/register'
import 'monaco-editor/languages/definitions/rust/register'
import 'monaco-editor/languages/definitions/sql/register'
import 'monaco-editor/languages/definitions/shell/register'
import 'monaco-editor/languages/definitions/html/register'
import 'monaco-editor/languages/definitions/css/register'
import 'monaco-editor/languages/definitions/markdown/register'
import 'monaco-editor/languages/definitions/xml/register'
import 'monaco-editor/languages/definitions/yaml/register'
import 'monaco-editor/languages/features/json/register.js'

// 自托管 monaco（不依赖 CDN）+ 基础 worker，避免控制台报错
self.MonacoEnvironment = {
  getWorker: () => new editorWorker(),
}
loader.config({ monaco })

// 扩展名 → monaco language id
const LANGUAGE_BY_EXT: Record<string, string> = {
  java: 'java',
  py: 'python',
  js: 'javascript',
  jsx: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  go: 'go',
  c: 'c',
  cpp: 'cpp',
  cc: 'cpp',
  cxx: 'cpp',
  h: 'cpp',
  hpp: 'cpp',
  cs: 'csharp',
  rb: 'ruby',
  php: 'php',
  kt: 'kotlin',
  swift: 'swift',
  rs: 'rust',
  sql: 'sql',
  sh: 'shell',
  html: 'html',
  css: 'css',
  vue: 'html',
  md: 'markdown',
  json: 'json',
  xml: 'xml',
  yml: 'yaml',
  yaml: 'yaml',
}

function detectLanguage(fileName?: string): string {
  if (!fileName) return 'plaintext'
  const ext = fileName.split('.').pop()?.toLowerCase() ?? ''
  return LANGUAGE_BY_EXT[ext] ?? 'plaintext'
}

interface CodeViewerProps {
  /** 代码内容，空则显示占位 */
  code?: string
  /** 文件名，用于自动识别语言 */
  fileName?: string
  /** 高亮定位行号（1 起） */
  activeLine?: number
  height?: string | number
}

function CodeViewer({ code = '', fileName, activeLine, height = '100%' }: CodeViewerProps) {
  const editorRef = useRef<Parameters<OnMount>[0] | null>(null)

  const handleMount: OnMount = (editor) => {
    editorRef.current = editor
  }

  // 点击问题定位：移动光标 + 居中显示目标行
  useEffect(() => {
    const editor = editorRef.current
    if (!editor || !activeLine || activeLine < 1) return
    editor.setPosition({ lineNumber: activeLine, column: 1 })
    editor.revealLineInCenter(activeLine)
  }, [activeLine])

  if (!code) {
    return (
      <div style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Empty description="暂无代码内容" />
      </div>
    )
  }

  return (
    <Editor
      height={height}
      language={detectLanguage(fileName)}
      value={code}
      theme="vs-dark"
      path={fileName}
      onMount={handleMount}
      options={{
        readOnly: true,
        domReadOnly: true,
        minimap: { enabled: false },
        lineNumbers: 'on',
        scrollBeyondLastLine: false,
        automaticLayout: true,
        fontSize: 13,
        wordWrap: 'off',
        contextmenu: false,
      }}
    />
  )
}

export default CodeViewer

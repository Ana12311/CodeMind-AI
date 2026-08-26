// 代码文件返回对象（对应后端 FileVO）
export interface FileItem {
  id: string
  projectId?: string
  fileName?: string
  filePath?: string
  fileType?: string
  // 字节数，后端 Long 序列化为字符串
  fileSize?: string
  storageUrl?: string
  checksum?: string
  status?: number
  createTime?: string
  updateTime?: string
}

// 分页查询文件（对应后端 FileQueryRequest）
export interface FileQuery {
  pageNum?: number
  pageSize?: number
  projectId?: string
  fileName?: string
}

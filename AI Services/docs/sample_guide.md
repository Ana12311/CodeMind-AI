# AI Service 使用指南

## 任务创建
调用 POST /api/tasks 创建 AI 任务，传入 taskId、taskType、content。

## LLM 调用
所有模型调用必须经过 LLM Service，禁止直接访问模型 API。

## RAG 检索
RAG 系统负责读取代码文档、切片、向量化、相似度检索。

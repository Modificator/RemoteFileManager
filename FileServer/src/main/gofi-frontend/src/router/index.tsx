import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import PageLoading from '../components/PageLoading'

const NotFound = lazy(() => import('../pages/exception/404'))
const UnAuthorized = lazy(() => import('../pages/exception/403'))
const ServerError = lazy(() => import('../pages/exception/500'))
const Setting = lazy(() => import('../pages/admin/setting/Index'))
const FileDetail = lazy(() => import('../pages/file/FileDetail'))
const FileViewer = lazy(() => import('../pages/file/FileViewer'))
const Login = lazy(() => import('../pages/Login'))
const Setup = lazy(() => import('../pages/Setup'))

declare global {
    interface Window {
        LOADED: boolean
    }
}

interface IProps {}

const defualtProps: IProps = {}

const GofiRouter: React.FC<IProps> = (props) => {
    return (
        <Suspense fallback={<PageLoading />}>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Navigate to="/file/viewer" replace={true} />} />
                    <Route path="/file/viewer" element={<FileViewer />} />
                    <Route path="/file/detail" element={<FileDetail />} />
                    <Route path="/404" element={<NotFound />} />
                    <Route path="/403" element={<UnAuthorized />} />
                    <Route path="/500" element={<ServerError />} />
                    <Route path="*" element={<Navigate to="/404" replace={true} />} />
                </Routes>
            </BrowserRouter>
        </Suspense>
    )
}

GofiRouter.defaultProps = defualtProps

export default GofiRouter

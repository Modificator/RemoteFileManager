import React from 'react'
import { useRecoilState } from 'recoil'
import { languageState } from '../../../states/common.state'
import Footer from '../../Footer'
import LangSelect from './LangSelect'
import LoginStatus from './LoginStatus'
import Logo from './Logo'
import NavMenu from './NavMenu'

const MainLayout: React.FC = (props) => {
    const [language, setLanguage] = useRecoilState(languageState)

    return (
        <div className="bg-gray-100 h-full w-full flex flex-col overflow-x-hidden">
            <div className="flex-grow w-full max-w-5xl mx-auto py-4 p-4 sm:p-0">{props.children}</div>
            <Footer />
        </div>
    )
}

export default MainLayout

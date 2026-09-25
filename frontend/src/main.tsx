import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { App as AntApp } from 'antd'
import App from './App'
import { AppProvider } from './store/AppContext'
import { FontProvider } from './store/FontContext'
import { ThemeProvider } from './theme'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <FontProvider>
      <ThemeProvider>
        <AntApp>
          <BrowserRouter>
            <AppProvider>
              <App />
            </AppProvider>
          </BrowserRouter>
        </AntApp>
      </ThemeProvider>
    </FontProvider>
  </React.StrictMode>,
)

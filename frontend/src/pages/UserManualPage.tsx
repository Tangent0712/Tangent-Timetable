import { Card, Collapse, Divider, Space, Typography } from 'antd'
import {
  BookOutlined,
  CheckCircleOutlined,
  QuestionCircleOutlined,
  RobotOutlined,
} from '@ant-design/icons'

const { Title, Paragraph, Text } = Typography

const FAQ = [
  {
    key: 'login',
    label: '登录不进去 / 提示 Key 无效？',
    children: '重新找管理员要一个有效 Key。',
  },
  {
    key: 'conflict',
    label: '课表格子为什么有的课没显示？',
    children:
      '同一时间段撞了两门课，系统只显示其中一门，并在格子上显示一个警告角标，点开能看到被顶掉的那门。去把时间调整开就好。',
  },
  {
    key: 'weeks',
    label: '周次怎么填不对？',
    children: '先在「上课周次」下面用 1-16 这类格式输入，点「应用」；或直接勾选。带 * 的是当前周。',
  },
  {
    key: 'exam',
    label: '考试怎么没在课表上？',
    children:
      '考试是按日期显示的，要翻到它所在的那一周才能看到（翻页或点「本周」）。如果日期不在学期范围内也可能看不到。',
  },
  {
    key: 'ai',
    label: 'AI 会乱改数据吗？',
    children:
      '不会。AI 只会生成「提案卡片」，必须你每条点「确认执行」才会真正改动，不点就什么都不变。',
  },
  {
    key: 'mobile',
    label: '手机上能用吗？',
    children: '能。所有页面都做了手机适配，窄屏会自动调整布局。',
  },
  {
    key: 'data',
    label: '数据安全吗？',
    children: '每个账号数据互相独立，用自己的 Key 登录只能看到自己的东西。',
  },
]

export default function UserManualPage() {
  return (
    <div className="manual-page">
      <Card
        size="small"
        title={
          <Space>
            <BookOutlined />
            <span>大切课程表 · 使用手册</span>
          </Space>
        }
        extra={<Text type="secondary">给非技术朋友的白话教程</Text>}
      >
        <div className="manual-body">
          <section>
            <Title level={4}>怎么进入</Title>
            <Paragraph>在电脑或手机浏览器打开：</Paragraph>
            <Paragraph copyable={{ text: window.location.origin }}>
              <Text code>{window.location.origin}</Text>
            </Paragraph>
            <Paragraph>手机也可以直接用，会自动适配成手机版样式。</Paragraph>
          </section>

          <Divider />

          <section>
            <Title level={4}>登录</Title>
            <Paragraph>
              输入自己的 <Text strong>API Key</Text>（找管理员要，一串像{' '}
              <Text code>tt_xxxxxxxx</Text> 的字符），点「登录」。一个 Key 就是一个独立账号，
              每个人看到的课表、待办都是自己的，互不影响。登录后右上角会显示你的名字。
            </Paragraph>
            <Paragraph type="secondary">忘了 Key？找管理员重新要一个，不影响已有数据。</Paragraph>
          </section>

          <Divider />

          <section>
            <Title level={4}>第一次进来先做什么</Title>
            <Paragraph>登录后进入「课表」页，如果还没有课表，点「新建课表」：</Paragraph>
            <ul>
              <li>课表名称：随便起，比如「大三上学期」。</li>
              <li>开始/结束日期：填这学期第一周周一的日期，和学期最后一天。</li>
              <li>一个账号可以建多个课表，左侧可以切换当前用哪个。</li>
            </ul>
          </section>

          <Divider />

          <section>
            <Title level={4}>看课表</Title>
            <ul>
              <li>默认「按周」视图，一周 7 天、每天最多 12 节课的格子。</li>
              <li>顶部可以翻页看第几周，点「本周」回到当前周。</li>
              <li>点右上角「全部课程」可按列表看所有课程。</li>
              <li>格子颜色 = 课程颜色，同一门课颜色固定，方便找。</li>
              <li>屏幕不够宽时课表自动变紧凑；手机上可左右滑动看完整网格。</li>
              <li>点格子可修改或删除这门课。</li>
            </ul>
          </section>

          <Divider />

          <section>
            <Title level={4}>添加 / 修改 / 删除课程</Title>
            <Paragraph>
              点「添加课程」填：课程名称、地点、老师、星期几、第几节、上课周次。
            </Paragraph>
            <Paragraph strong>周次快捷输入：</Paragraph>
            <ul>
              <li>
                <Text code>1-16</Text> = 第 1 到 16 周都有课
              </li>
              <li>
                <Text code>1,3,5</Text> = 单周
              </li>
              <li>有「单周」「双周」「仅本周」「全选」按钮，输完点「应用」。</li>
            </ul>
            <Paragraph>
              修改：点课程格子 → 改完保存。删除：点课程格子 → 删除。
              <br />
              批量导入：有教务 HTML 课表可点「导入课表」→ 粘贴 → AI 识别 → 勾选 → 导入。
            </Paragraph>
          </section>

          <Divider />

          <section>
            <Title level={4}>
              <Space>
                <CheckCircleOutlined />
                考试记录（期末周用）
              </Space>
            </Title>
            <Paragraph>
              考试不按课时算，直接填日期和起止时间，适合期末周、四六级、普通话这类考试。
            </Paragraph>
            <ul>
              <li>课表页点「考试管理」→「添加考试」，填名称、日期、起止时间、地点。</li>
              <li>考试会按日期时间在课表上显示一个红色块，块上标名称、起止时间（两行）、地点。</li>
              <li>
                每加一场考试会自动生成一条待办（截止 = 考试开始时间），列表里标红色「考试」；
                考试结束后自动标记完成。
              </li>
            </ul>
          </section>

          <Divider />

          <section>
            <Title level={4}>待办事项（提醒自己）</Title>
            <ul>
              <li>新建待办：写标题 + 选截止时间。</li>
              <li>勾选左边方框 = 标记完成。</li>
              <li>每条待办显示倒计时，快到期变色提醒。</li>
              <li>「循环任务」标签页可建每周/每月重复的提醒（进阶功能）。</li>
            </ul>
          </section>

          <Divider />

          <section>
            <Title level={4}>
              <Space>
                <RobotOutlined />
                AI 助手（用一句话办事）
              </Space>
            </Title>
            <Paragraph>不用手动点，用一句话让 AI 帮你干活。例如：</Paragraph>
            <ul>
              <li>「下周一3-4节加一节形势政策，地点待定」</li>
              <li>「把周三的高数从第3-4节改到第5-6节」</li>
              <li>「删掉周五上午的课，再加一条周三交实验报告的待办」</li>
              <li>「12月16号加一场数据结构期末考试，下午2点到4点」</li>
            </ul>
            <Paragraph strong>步骤：</Paragraph>
            <ol>
              <li>输入想做的事，按发送（Enter 发送，Shift+Enter 换行）。</li>
              <li>AI 回复文字，并给出准备执行的「操作卡片」。</li>
              <li>每条操作都要你点「确认执行」才会真正改动；不想执行点「取消」。</li>
              <li>一次说多件事会生成多张卡片，逐条确认。</li>
            </ol>
            <Paragraph type="secondary">
              每次对话都绑定一个课表（工作空间）。切到别的课表时输入和操作会锁定，防止改错地方。
            </Paragraph>
          </section>

          <Divider />

          <section>
            <Title level={4}>设置</Title>
            <ul>
              <li>
                <Text strong>作息时间表</Text>
                ：全校统一上下课时间，仅供阅读、不可编辑（管理员才能改）。
              </li>
              <li>
                <Text strong>字体大小</Text>
                ：可分别调「全局其它字体」和「课程表字体」，只保存在当前设备。
              </li>
            </ul>
          </section>

          <Divider />

          <section>
            <Title level={4}>
              <Space>
                <QuestionCircleOutlined />
                常见问题
              </Space>
            </Title>
            <Collapse
              items={FAQ}
              size="small"
              style={{ background: 'transparent' }}
              bordered={false}
            />
          </section>

          <Divider />

          <Paragraph type="secondary" style={{ textAlign: 'center' }}>
            有问题随时找管理员反馈 🙂
          </Paragraph>
        </div>
      </Card>
    </div>
  )
}

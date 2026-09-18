import { icon as i } from "./icons.js?v=2";
const query = new URLSearchParams(location.search);
const concepts = ["focus", "mission", "studio", "paper", "pulse"];
const concept = concepts.includes(query.get("concept"))
  ? query.get("concept")
  : "focus";
const state = {
  screen:
    concept === "mission" ? "tasks" : concept === "pulse" ? "voice" : "chat",
  scenario: ["ready", "offline"].includes(query.get("scenario"))
    ? query.get("scenario")
    : "approval",
  font: [1, 1.5, 2].includes(Number(query.get("font")))
    ? Number(query.get("font"))
    : 1,
  draft: "",
  messages: [],
  approval: "pending",
  session: "한국어 UI 점검",
  artifact: "changes",
  listening: false,
  connection: "내 Hermes",
  newChat: false,
  taskFilter: "all",
  pane: "chat",
};
const $ = (s) => document.querySelector(s);
const esc = (s) =>
  String(s).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const btn = (action, label, icon = "", cls = "") =>
  `<button type="button" class="btn ${cls}" data-action="${action}">${icon ? i(icon) : ""}<span>${label}</span></button>`;
const ib = (action, label, icon) =>
  `<button type="button" class="icon-btn" data-action="${action}" aria-label="${label}">${i(icon)}</button>`;
const online = () => state.scenario !== "offline";
const pending = () =>
  state.scenario === "approval" && state.approval === "pending";
const badge = (label, cls = "") => `<span class="badge ${cls}">${label}</span>`;
const mark = () => `<span class="computer-icon">${i("computer")}</span>`;
function header() {
  const title =
    state.screen === "chat"
      ? state.session
      : { tasks: "작업", connections: "연결", voice: "음성 메모" }[
          state.screen
        ];
  return `<header class="app-header"><button class="nav-back" data-action="sessions" aria-label="대화 목록">${i("back")}<span>대화</span></button><button class="connection-title" data-action="connection"><strong>${esc(title)}</strong><small><b class="status-dot ${online() ? "" : "disconnected"}"></b>${esc(state.connection)}${online() ? "" : " · 오프라인"}</small></button><div class="header-actions">${ib("new-chat", "새 대화", "compose")}${ib("settings", "화면 설정", "dots")}</div></header>`;
}
function side() {
  return `<aside class="session-sidebar"><div class="sidebar-heading"><a href="index.html" target="_top">Hermes Connect</a>${ib("new-chat", "새 대화", "compose")}</div><button class="sidebar-search" data-action="sessions">${i("search")} 대화 검색</button><p class="sidebar-label">오늘</p>${["한국어 UI 점검", "연결 안정성 개선", "주간 작업 요약"].map((s, n) => `<button class="session-item ${state.session === s ? "active" : ""}" data-action="session:${n}"><span><strong>${s}</strong><small>${["파일 3개 · 승인 대기", "재연결 시나리오 확인 중", "작업 2개 완료"][n]}</small></span><time>${["10:43", "10:30", "09:15"][n]}</time></button>`).join("")}<div class="sidebar-bottom">${btn("screen:connections", "연결", "link")}<small>예시 데이터</small></div></aside>`;
}
function nav() {
  const items =
    concept === "pulse"
      ? [
          ["voice", "음성", "mic"],
          ["tasks", "작업", "list"],
          ["chat", "대화", "chat"],
          ["connections", "연결", "link"],
        ]
      : [
          ["chat", "대화", "chat"],
          ["tasks", "작업", "list"],
          ["connections", "연결", "link"],
        ];
  return `<nav class="bottom-nav" aria-label="주요 메뉴">${items.map(([id, label, icon]) => `<button data-action="screen:${id}" ${state.screen === id ? 'aria-current="page"' : ""}>${i(icon)}<span>${label}</span>${id === "tasks" && pending() ? '<b class="nav-count" aria-label="승인 대기 1개">1</b>' : ""}</button>`).join("")}</nav>`;
}
function offline() {
  return online()
    ? ""
    : `<div class="offline-banner" role="status"><span>연결 끊김 · 작성한 내용은 유지됩니다.</span>${btn("reconnect", "재연결")}</div>`;
}
function approval(compact = false) {
  if (!pending())
    return state.approval === "pending"
      ? ""
      : `<div class="decision-receipt">${i(state.approval === "approved" ? "check" : "close")}변경 요청을 ${state.approval === "approved" ? "승인" : "거절"}했습니다.</div>`;
  return `<section class="approval-card ${compact ? "compact" : ""}" aria-label="승인 대기"><div class="approval-heading"><span class="approval-icon">${i("shield")}</span><div><strong>파일 변경 승인</strong><small>파일 3개 · 이번 작업에만 적용</small></div></div>${compact ? "" : "<p>변경 내용을 확인한 뒤 승인해 주세요.</p>"}<div class="approval-actions">${btn("review", "변경 내용 보기", "", "primary")}</div></section>`;
}
function focusApproval() {
  return pending()
    ? `<button class="focus-approval" data-action="review"><span class="approval-icon">${i("shield")}</span><span><strong>승인 요청</strong><small>파일 3개 변경</small></span><span class="review-link">검토</span>${i("chevron")}</button>`
    : approval(true);
}
function composer() {
  return `<div class="composer-wrap">${concept === "focus" ? focusApproval() : ""}<form class="composer">${ib("attach", "파일 첨부", "plus")}<div class="input-shell"><label class="sr-only" for="message">메시지</label><textarea id="message" rows="1" placeholder="${concept === "paper" ? "후속 질문" : "메시지"}">${esc(state.draft)}</textarea>${ib("voice", "음성 입력 시안", "mic")}<button class="send-button" type="submit" aria-label="메시지 보내기" ${online() ? "" : "disabled"}>${i("send")}</button></div></form><small class="composer-note">${online() ? "시안 · 메시지가 실제로 전송되지 않습니다" : "다시 연결한 뒤 보낼 수 있습니다"}</small></div>`;
}
function reportContent() {
  return `<p>화면 3개를 확인했습니다. 다음 변경을 제안합니다.</p><ul class="report-list"><li><strong>대화</strong><span>상단 연결 정보를 한 줄로 정리</span></li><li><strong>관리</strong><span>승인 대기 항목을 목록 위에 표시</span></li><li><strong>펼친 화면</strong><span>대화 옆에 변경 파일 표시</span></li></ul><p>아래 파일을 검토해 주세요.</p>`;
}
function chat() {
  const empty = `<div class="empty-state"><h1>새 대화</h1><p>${esc(state.connection)}에 연결됨</p>${btn("seed-prompt", "한국어 화면 점검", "plus", "outlined")}</div>`;
  const messageView = `<div class="conversation-date">오늘 오전 10:42</div><div class="user-message">한국어 문구와 버튼 배치를 확인해 줘.</div><div class="delivered">전달됨</div><article class="assistant-message"><div class="message-author">Hermes <time>10:43</time></div><div class="response-content">${reportContent()}</div><button class="artifact-card" data-action="open-result"><span class="document-icon">${i("file")}</span><span><strong>UI 검토 노트</strong><small>문서 · 변경 제안 3개</small></span>${i("chevron")}</button><div class="message-meta"><span>검토 완료</span>${ib("copy", "검토 요약 복사", "copy")}</div></article>`;
  const noteView = `<article class="paper-document"><div class="note-date">오늘 오전 10:43</div><h1>한국어 UI 점검</h1><p class="note-lead">대화, 관리, 연결 화면 검토</p><h2>검토 내용</h2><p>상단의 연결 정보와 모델 선택이 대화 공간을 많이 차지합니다. 자주 쓰는 기능부터 보이도록 정리합니다.</p><h2>변경할 항목</h2><ul class="note-checklist"><li><span></span>연결 이름과 상태를 한 줄로 표시</li><li><span></span>승인 대기 항목을 입력창 위에 배치</li><li><span></span>펼친 화면에서 변경 파일 함께 표시</li></ul><h2>관련 파일</h2><button class="note-attachment" data-action="open-result">${i("file")}<span>UI 검토 노트<small>변경 제안 3개</small></span>${i("chevron")}</button><p class="note-caption">${pending() ? "변경 전 확인 필요" : "검토 완료"} · 파일 3개</p></article>${focusApproval()}`;
  return `<section class="chat-panel"><div class="chat-scroll">${state.newChat ? empty : concept === "paper" ? noteView : messageView}${state.messages.map((m) => `<div class="${m.role === "user" ? "user-message" : "local-reply"}">${esc(m.text)}</div>`).join("")}</div>${composer()}</section>`;
}
function tasks() {
  const rows = [
    {
      title: "한국어 UI 점검",
      sub: pending() ? "파일 3개 변경 승인 필요" : "검토 완료",
      kind: pending() ? "approval" : "done",
      action: "review",
    },
    {
      title: "연결 안정성 개선",
      sub: "재연결 시나리오 확인 중",
      kind: "running",
      action: "task-detail",
    },
    {
      title: "주간 작업 요약",
      sub: "오늘 오전 9:15",
      kind: "done",
      action: "summary",
    },
  ];
  const filtered = rows.filter(
    (r) => state.taskFilter === "all" || r.kind === state.taskFilter,
  );
  const list = (items) =>
    items
      .map(
        (r) =>
          `<button class="task-row" data-action="${r.action}"><span class="task-circle ${r.kind}">${r.kind === "done" ? i("check") : r.kind === "running" ? i("clock") : ""}</span><span class="task-text"><strong>${r.title}</strong><small>${r.sub}</small></span>${i("chevron")}</button>`,
      )
      .join("");
  return `<section class="task-panel scroll-panel"><div class="page-heading"><h1>작업</h1><p>오늘</p></div><div class="task-filters" role="group" aria-label="작업 필터">${[
    ["all", "전체"],
    ["approval", "승인 대기"],
    ["running", "진행 중"],
  ]
    .map(
      ([id, label]) =>
        `<button data-action="filter:${id}" aria-pressed="${state.taskFilter === id}">${label}</button>`,
    )
    .join(
      "",
    )}</div>${filtered.some((r) => r.kind !== "done") ? `<div class="section-heading"><h2>${state.taskFilter === "all" ? "진행 중" : "확인할 항목"}</h2><span>${filtered.filter((r) => r.kind !== "done").length}</span></div><div class="task-list">${list(filtered.filter((r) => r.kind !== "done"))}</div>` : ""}${filtered.some((r) => r.kind === "done") ? `<div class="section-heading"><h2>완료됨</h2><span>${filtered.filter((r) => r.kind === "done").length}</span></div><div class="task-list completed-list">${list(filtered.filter((r) => r.kind === "done"))}</div>` : ""}${!filtered.length ? '<p class="list-empty">해당하는 작업이 없습니다.</p>' : ""}<div class="list-footer">${btn("new-chat", "새 작업", "plus")}</div></section>`;
}
function timeline() {
  return `<ol class="timeline"><li><span class="timeline-dot done"></span><div><strong>요청 수신</strong><small>10:42</small></div></li><li><span class="timeline-dot done"></span><div><strong>화면 3개 검토</strong><small>10:42</small></div></li><li><span class="timeline-dot ${pending() ? "waiting" : "done"}"></span><div><strong>${pending() ? "변경 승인 대기" : "검토 완료"}</strong><small>10:43</small></div></li></ol>`;
}
function context() {
  return `<aside class="context-rail"><h2>세부사항</h2><section class="context-task"><h3>한국어 UI 점검</h3><dl><dt>연결</dt><dd>${esc(state.connection)}</dd><dt>상태</dt><dd>${pending() ? "승인 대기" : "검토 완료"}</dd><dt>파일</dt><dd>3개</dd></dl></section><h3 class="small-heading">활동</h3>${timeline()}<h3 class="small-heading">첨부 파일</h3><button class="file-row" data-action="open-result">${i("file")}<span>UI 검토 노트<small>변경 제안 3개</small></span>${i("chevron")}</button></aside>`;
}
function artifact() {
  const diff = state.artifact === "diff";
  const selected = diff ? "changes" : state.artifact;
  return `<aside class="artifact-pane"><div class="result-heading"><strong>작업 파일</strong>${ib("copy", "검토 요약 복사", "copy")}</div><div class="result-tabs" role="tablist" aria-label="결과 종류">${[
    ["changes", "파일"],
    ["note", "노트"],
    ["terminal", "기록"],
  ]
    .map(
      ([id, label]) =>
        `<button role="tab" aria-selected="${selected === id}" data-action="artifact:${id}">${label}</button>`,
    )
    .join(
      "",
    )}</div><div class="artifact-scroll" role="tabpanel" aria-label="작업 결과">${
    diff
      ? `<button class="file-back" data-action="artifact:changes">${i("back")} 파일</button><h2 class="file-name">ChatScreen.kt</h2><p class="file-description">변경 예시</p><pre class="code-diff"><code><span class="code-context">  Column {</span>
<span class="code-minus">−   ConnectionDetails()</span>
<span class="code-plus">+   CompactConnectionStatus()</span>
<span class="code-context">    ChatContent()</span>
<span class="code-plus">+   ApprovalBar()</span>
<span class="code-context">    MessageComposer()</span>
<span class="code-context">  }</span></code></pre>`
      : state.artifact === "changes"
        ? `<div class="file-breadcrumb">Hermes Connect ${i("chevron")} 한국어 UI 점검</div><h2 class="files-title">변경 파일</h2><div class="file-list">${[
            ["ChatScreen.kt", "대화 화면 배치", "+2 −1"],
            ["ConnectionHeader.kt", "연결 상태 요약", "+8 −12"],
            ["ApprovalBar.kt", "승인 요청 표시", "+14"],
          ]
            .map(
              ([name, detail, count]) =>
                `<button class="file-item" data-action="file:${name}"><span class="document-icon">${i("file")}</span><span><strong>${name}</strong><small>${detail}</small></span><span class="file-count">${count}</span>${i("chevron")}</button>`,
            )
            .join(
              "",
            )}</div><p class="file-summary">3개 파일 · ${pending() ? "승인 대기" : "검토 완료"}</p><h3 class="small-heading">문서</h3><button class="file-item" data-action="artifact:note"><span class="document-icon note">${i("file")}</span><span><strong>UI 검토 노트</strong><small>문서 · 오늘 오전 10:43</small></span>${i("chevron")}</button>`
        : state.artifact === "note"
          ? `<article class="note-document"><div class="note-date">오늘 오전 10:43</div><h2>UI 검토 노트</h2>${reportContent()}</article>`
          : `<div class="terminal"><p><time>10:42:01</time> 요청 수신</p><p><time>10:42:03</time> 화면 구성 확인</p><p><time>10:42:08</time> 한국어 문구 검토</p><p><time>10:42:19</time> 변경 제안 준비</p><p>${pending() ? "승인 대기" : "검토 완료"}</p></div>`
  }</div><div class="artifact-approval">${focusApproval()}</div></aside>`;
}
function connections() {
  return `<section class="connections-panel scroll-panel"><div class="page-heading"><h1>연결</h1></div><div class="settings-group"><button class="settings-row connection-profile" data-action="connection-details"><span class="computer-icon">${i("computer")}</span><span><strong>${esc(state.connection)}</strong><small>${online() ? "연결됨" : "오프라인"}</small></span>${i("chevron")}</button><button class="settings-row" data-action="connection-details"><span>연결 경로</span><span class="row-value">Tailscale</span>${i("chevron")}</button><button class="settings-row" data-action="model"><span>모델</span><span class="row-value">서버 기본값</span>${i("chevron")}</button></div><h2 class="settings-label">사용 가능한 기능</h2><div class="settings-group">${[
    ["chat", "대화"],
    ["file", "파일"],
    ["mic", "음성"],
  ]
    .map(
      ([icon, name]) =>
        `<div class="settings-row"><span class="setting-icon">${i(icon)}</span><span>${name}</span><span class="row-value">사용 가능</span></div>`,
    )
    .join(
      "",
    )}<button class="settings-row" data-action="relay"><span class="setting-icon">${i("terminal")}</span><span>Relay 확장</span><span class="row-value">설정</span>${i("chevron")}</button></div><p class="group-caption">터미널과 데스크톱 도구는 Relay 페어링 후 사용할 수 있습니다.</p><div class="settings-group">${btn("add-connection", "연결 추가", "plus", "wide")}</div><p class="group-caption">시안용 연결 정보입니다.</p></section>`;
}
function voice() {
  const bars = [
    8, 14, 24, 12, 20, 37, 54, 28, 45, 65, 42, 24, 51, 72, 46, 27, 36, 57, 35,
    18, 31, 44, 26, 16, 34, 52, 28, 17, 24, 13, 20, 8,
  ];
  return `<section class="voice-panel"><div class="voice-content"><div class="page-heading"><h1>음성 메모</h1></div><p class="section-caption">오늘</p><div class="recording-list"><button class="recording-row" data-action="recording:한국어 UI 점검"><span><strong>한국어 UI 점검</strong><small>오전 10:42</small></span><time>0:18</time>${i("chevron")}</button><button class="recording-row" data-action="recording:오늘 할 일"><span><strong>오늘 할 일</strong><small>오전 9:15</small></span><time>0:32</time>${i("chevron")}</button></div>${pending() ? `<button class="voice-current" data-action="review">${i("shield")}<span>한국어 UI 점검<small>파일 변경 승인 필요</small></span>${i("chevron")}</button>` : ""}</div><div class="recorder ${state.listening ? "recording" : ""}"><div class="recorder-heading"><strong>${state.listening ? "녹음 화면 미리보기" : "새 음성 메모"}</strong><span>${state.listening ? "00:08" : "00:00"}</span></div><div class="waveform" aria-hidden="true">${bars.map((h) => `<span style="--bar-height:${state.listening ? h : Math.max(3, h / 4)}px"></span>`).join("")}<div class="playhead"></div></div><div class="record-control"><button class="record-button ${state.listening ? "recording" : ""}" data-action="listen" aria-label="${state.listening ? "녹음 시안 중지" : "녹음 시안 시작"}"><span></span></button></div><small class="composer-note">시안 · 마이크를 사용하지 않습니다</small></div></section>`;
}
function render() {
  document.body.className = `theme-${concept}`;
  document.documentElement.dataset.largeText = String(state.font > 1);
  document.documentElement.style.setProperty("--font-scale", state.font);
  const chatView = state.screen === "chat";
  $("#app").innerHTML =
    `<div class="app-shell ${concept}">${side()}<main class="app-main">${header()}${offline()}${concept === "studio" && chatView ? `<div class="studio-switch" role="tablist" aria-label="작업 패널"><button role="tab" aria-selected="${state.pane === "chat"}" data-action="studio-chat">대화</button><button role="tab" aria-selected="${state.pane === "result"}" data-action="studio-result">파일 <span>3</span></button></div>` : ""}<div class="workspace ${chatView ? "chat-workspace" : ""} ${state.pane === "result" ? "show-result" : ""}">${state.screen === "tasks" ? tasks() : state.screen === "connections" ? connections() : state.screen === "voice" ? voice() : chat()}${concept === "studio" && chatView ? artifact() : state.screen === "connections" ? "" : context()}</div>${nav()}</main></div>`;
  const input = $("#message");
  if (input) {
    input.addEventListener("input", () => (state.draft = input.value));
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey && !e.isComposing) {
        e.preventDefault();
        send();
      }
    });
    $(".composer").addEventListener("submit", (e) => {
      e.preventDefault();
      send();
    });
  }
  reportSize();
}
function send() {
  if (!online() || !state.draft.trim()) return;
  state.messages.push(
    { role: "user", text: state.draft.trim() },
    {
      role: "assistant",
      text: "메시지를 추가했습니다. 실제 하네스에는 전송되지 않습니다.",
    },
  );
  state.draft = "";
  render();
  $(".chat-scroll")?.scrollTo(0, $(".chat-scroll").scrollHeight);
  $("#message")?.focus();
}
let toastTimer;
function toast(text) {
  $("#toast").textContent = text;
  $("#toast").classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => $("#toast").classList.remove("visible"), 3200);
}
function sheet(title, html) {
  const dialog = $("#sheet");
  dialog.innerHTML = `<div class="sheet-header"><h2 id="sheet-title">${title}</h2>${ib("close-sheet", "닫기", "close")}</div><div class="sheet-content">${html}</div>`;
  if (!dialog.open) dialog.showModal();
}
function review() {
  sheet(
    "파일 변경 승인",
    `<div class="sheet-callout">${i("shield")}<div><strong>한국어 UI 점검</strong><p>파일 3개 수정 · 이번 작업에만 적용</p></div></div><p>상단 연결 정보를 간결하게 정리하고 입력창 가까이에 승인 요청을 표시하는 예시입니다.</p><ul class="review-files"><li>ChatScreen.kt <span>대화 화면 배치</span></li><li>ConnectionHeader.kt <span>연결 상태 요약</span></li><li>ApprovalBar.kt <span>승인 대기 표시</span></li></ul><p class="muted-note">동작 확인용 시안입니다. 승인해도 실제 파일이나 권한은 변경되지 않습니다.</p>${pending() ? `<div class="sheet-actions">${btn("deny", "거절")}${online() ? btn("approve", "이번 변경 승인", "check", "primary") : btn("reconnect", "다시 연결", "link", "primary")}</div>` : '<p class="decision-receipt">현재 승인 대기 중인 요청이 없습니다.</p>'}`,
  );
}
function resultSheet() {
  sheet(
    "모바일 UI 검토 노트",
    `<div class="note-document"><h3>대화 화면</h3><p>상단에는 활성 연결과 상태만 남기고, 경로·모델 등 상세 정보는 필요할 때 펼칩니다.</p><h3>변경 전에 확인하기</h3><p>파일 3개의 변경 범위를 확인하고 이번 작업에만 승인합니다.</p><h3>펼친 화면 활용하기</h3><p>대화와 결과를 나란히 표시합니다. 화면 크기가 바뀌어도 작성 중인 메시지는 유지합니다.</p></div>${pending() ? btn("review", "파일 변경 확인", "shield", "primary wide") : ""}`,
  );
}
document.addEventListener("click", (e) => {
  const target = e.target.closest("[data-action]");
  if (!target) return;
  const action = target.dataset.action;
  if (action.startsWith("filter:")) {
    state.taskFilter = action.split(":")[1];
    render();
    return;
  }
  if (action.startsWith("file:")) {
    if (action === "file:ChatScreen.kt") {
      state.artifact = "diff";
      render();
    } else {
      sheet(
        action.slice(5),
        '<p>연결 상태와 승인 요청의 표시를 정리하는 파일입니다.</p><p class="muted-note">시안용 파일 정보입니다. 실제 소스는 변경하지 않습니다.</p>',
      );
    }
    return;
  }
  if (action.startsWith("recording:")) {
    sheet(
      action.slice(10),
      '<div class="note-document"><p>한국어 문구와 버튼 배치를 확인해 줘.</p><p class="muted-note">예시 음성 메모의 텍스트입니다. 재생할 녹음 파일은 없습니다.</p></div>',
    );
    return;
  }

  if (action.startsWith("screen:")) {
    state.screen = action.split(":")[1];
    render();
    return;
  }
  if (action.startsWith("artifact:")) {
    state.artifact = action.split(":")[1];
    const showing = $(".workspace")?.classList.contains("show-result");
    render();
    if (showing) showStudioResult(true);
    return;
  }
  if (action.startsWith("session:")) {
    $("#sheet").close();
    state.session = ["한국어 UI 점검", "연결 안정성 개선", "주간 작업 요약"][
      Number(action.split(":")[1])
    ];
    state.screen = "chat";
    state.newChat = state.session !== "한국어 UI 점검";
    state.messages = [];
    render();
    return;
  }
  if (action.startsWith("font:")) {
    state.font = Number(action.split(":")[1]);
    $("#sheet").close();
    render();
    return;
  }
  switch (action) {
    case "sessions":
      sheet(
        "대화",
        `<div class="session-list">${["한국어 UI 점검", "연결 안정성 개선", "주간 작업 요약"].map((title, n) => btn("session:" + n, title, "chat", "wide")).join("")}</div>`,
      );
      break;
    case "close-sheet":
      $("#sheet").close();
      break;
    case "connection":
    case "connection-details":
      sheet(
        "연결 상태",
        `<div class="sheet-callout">${mark()}<div><strong>${esc(state.connection)}</strong><p>${online() ? "연결됨 · 예시 연결" : "오프라인 · 예시 상태"}</p></div></div><dl class="details"><dt>기본 연결</dt><dd>Dashboard / Gateway</dd><dt>현재 경로</dt><dd>Tailscale · HTTPS</dd><dt>사용 가능</dt><dd>대화 · 파일 · 기본 음성</dd><dt>Relay 확장</dt><dd>페어링하지 않음</dd></dl>${btn("manage-connections", "연결 관리", "arrow", "primary wide")}`,
      );
      break;
    case "manage-connections":
      $("#sheet").close();
      state.screen = "connections";
      render();
      break;
    case "review":
      review();
      break;
    case "approve":
      if (!online() || !pending()) return;
      state.approval = "approved";
      $("#sheet").close();
      render();
      toast("예시 변경을 승인했습니다. 실제 작업은 실행하지 않습니다.");
      break;
    case "deny":
      state.approval = "denied";
      $("#sheet").close();
      render();
      toast("예시 요청을 거절했습니다.");
      break;
    case "reconnect":
      state.scenario = "approval";
      $("#sheet").close();
      render();
      toast("연결됨 상태로 전환했습니다.");
      break;
    case "new-chat":
      state.screen = "chat";
      state.newChat = true;
      state.session = "새 대화";
      state.messages = [];
      render();
      $("#message")?.focus();
      break;
    case "seed-prompt":
      state.draft = "한국어 화면을 점검해 줘";
      render();
      $("#message")?.focus();
      break;
    case "open-result":
      if (concept === "studio") {
        state.artifact = "note";
        render();
        showStudioResult(true);
      } else resultSheet();
      break;
    case "studio-result":
      showStudioResult(true);
      break;
    case "studio-chat":
      showStudioResult(false);
      break;
    case "task-detail":
      sheet(
        "연결 안정성 개선",
        `<div class="sheet-callout">${i("activity")}<div><strong>재연결 시나리오 검증 중</strong><p>예시 진행률 68%</p></div></div><p>연결이 끊긴 동안 입력을 보존하고, 동일한 세션에서 작업을 이어가는 흐름입니다.</p>${timeline()}${btn("open-task", "대화로 이동", "chat", "primary wide")}`,
      );
      break;
    case "open-task":
      $("#sheet").close();
      state.screen = "chat";
      render();
      break;
    case "summary":
      sheet(
        "오늘의 작업 기록",
        '<div class="note-document"><h3>완료한 작업 2개</h3><p>한국어 문구 점검과 화면 구성 검토를 마쳤습니다. 다음으로 연결 상태와 승인 위치를 정리할 차례입니다.</p><p class="muted-note">시안용 예시 기록입니다.</p></div>',
      );
      break;
    case "attach":
      sheet(
        "파일 첨부",
        `<p>예시 문서를 대화에 첨부해 보세요.</p>${btn("attach-demo", "UI 검토 노트.md 첨부", "file", "outlined wide")}<p class="muted-note">실제 파일을 읽거나 업로드하지 않습니다.</p>`,
      );
      break;
    case "attach-demo":
      state.draft =
        (state.draft ? state.draft + "\n" : "") +
        "[첨부 예시: UI 검토 노트.md]";
      $("#sheet").close();
      render();
      break;
    case "model":
      sheet(
        "대화 모델",
        '<div class="sheet-callout"><strong>서버 기본 모델</strong></div><p>연결한 하네스의 설정을 따릅니다. 모델과 제공업체 선택은 실제 연동 단계에서 연결합니다.</p><p class="muted-note">이 시안은 모델 API를 호출하지 않습니다.</p>',
      );
      break;
    case "voice":
      sheet(
        "음성 입력",
        `<p>음성 입력의 위치와 흐름을 확인하는 시안입니다.</p>${btn("voice-demo", "예시 문장 입력", "mic", "primary wide")}<p class="muted-note">마이크 권한이나 음성 서비스를 사용하지 않습니다.</p>`,
      );
      break;
    case "voice-demo":
      state.draft = "지금 진행 중인 작업을 요약해 줘";
      $("#sheet").close();
      render();
      break;
    case "listen":
      state.listening = !state.listening;
      render();
      break;
    case "copy":
      navigator.clipboard
        ?.writeText(
          "UI 검토: 연결 상태 요약, 명시적 승인, 대화와 결과의 분할 화면.",
        )
        .then(() => toast("검토 요약을 복사했습니다."))
        .catch(() => toast("이 브라우저에서는 복사할 수 없습니다."));
      break;
    case "settings":
      sheet(
        "화면 설정",
        `<p>큰 글자에서도 모든 내용을 읽고 조작할 수 있는지 확인하세요.</p><div class="font-options">${[1, 1.5, 2].map((n) => btn("font:" + n, `${n * 100}%`, n === state.font ? "check" : "", n === state.font ? "primary" : "outlined")).join("")}</div><p class="muted-note">현재 시안: ${concept.toUpperCase()} · 예시 데이터</p><a class="text-link" href="index.html?concept=${concept}" target="_top">다섯 가지 방향 비교하기 ↗</a>`,
      );
      break;
    case "relay":
      sheet(
        "Relay로 확장하기",
        `<p>표준 대화와 기본 음성은 Dashboard/Gateway를 통해 사용합니다.</p><p>터미널과 데스크톱 도구는 선택적으로 Relay를 페어링하고 필요한 권한을 허용한 뒤 사용합니다.</p><p class="muted-note">이 화면에서는 실제 페어링을 시작하지 않습니다.</p>`,
      );
      break;
    case "add-connection":
      sheet(
        "다른 Hermes 연결",
        `<label class="field-label" for="connection-name">연결 이름</label><input id="connection-name" class="text-field" placeholder="예: 작업용 Hermes" maxlength="40"><p class="muted-note">시안에서는 이름만 바꿉니다. 실제 서버에는 연결하지 않습니다.</p>${btn("demo-connect", "예시 연결 추가", "link", "primary wide")}`,
      );
      break;
    case "demo-connect":
      state.connection = $("#connection-name").value.trim() || "작업용 Hermes";
      $("#sheet").close();
      render();
      toast("예시 연결 이름을 변경했습니다.");
      break;
  }
});
function showStudioResult(show) {
  state.pane = show ? "result" : "chat";
  $(".workspace")?.classList.toggle("show-result", show);
  document
    .querySelectorAll(".studio-switch button")
    .forEach((b, n) =>
      b.setAttribute("aria-selected", String(n === (show ? 1 : 0))),
    );
}
$("#sheet").addEventListener("click", (e) => {
  if (e.target === $("#sheet")) {
    const r = e.target.getBoundingClientRect();
    if (
      e.clientX < r.left ||
      e.clientX > r.right ||
      e.clientY < r.top ||
      e.clientY > r.bottom
    )
      e.target.close();
  }
});
function reportSize() {
  if (parent !== window)
    parent.postMessage(
      { type: "preview-size", width: innerWidth, height: innerHeight },
      location.origin,
    );
}
window.addEventListener("resize", reportSize);
window.addEventListener("message", (e) => {
  if (
    e.origin !== location.origin ||
    e.source !== parent ||
    e.data?.type !== "preview-settings"
  )
    return;
  if (
    ["approval", "ready", "offline"].includes(e.data.scenario) &&
    state.scenario !== e.data.scenario
  ) {
    state.scenario = e.data.scenario;
    state.approval = "pending";
  }
  if ([1, 1.5, 2].includes(e.data.font)) state.font = e.data.font;
  $("#sheet").close();
  render();
});
render();

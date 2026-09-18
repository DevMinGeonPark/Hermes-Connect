import { icon as i } from "./icons.js";
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
const mark = () =>
  '<span class="brand-mark"><img src="assets/hermes.svg" alt=""></span>';
function header() {
  return `<header class="app-header"><button class="connection-title" data-action="connection">${mark()}<span><strong>${esc(state.connection)} ${i("down")}</strong><small><b class="status-dot ${online() ? "" : "disconnected"}"></b>${online() ? "연결됨" : "연결 끊김"} <span class="demo-label">· 시안</span></small></span></button><div class="header-actions">${ib("new-chat", "새 대화", "plus")}${ib("settings", "화면 설정", "sliders")}</div></header>`;
}
function side() {
  return `<aside class="session-sidebar"><a class="sidebar-brand" href="index.html" target="_top">${mark()}<span>hermes<strong>connect</strong></span></a>${btn("new-chat", "새 대화", "plus", "primary")}<p class="eyebrow">WORKSPACE</p><div class="project-label">${i("folder")} Hermes Connect</div><p class="eyebrow">최근 대화</p>${["한국어 UI 점검", "연결 안정성 개선", "주간 작업 요약"].map((s, n) => `<button class="session-item ${state.session === s ? "active" : ""}" data-action="session:${n}">${i(n === 0 ? "chat" : "file")}<span>${s}</span>${n === 0 ? '<span class="tiny-dot"></span>' : ""}</button>`).join("")}<div class="sidebar-bottom">${btn("screen:connections", "연결 관리", "link")}<small>내 하네스와 이어지는 공간</small></div></aside>`;
}
function nav() {
  const items =
    concept === "pulse"
      ? [
          ["voice", "음성", "mic"],
          ["tasks", "작업", "grid"],
          ["chat", "대화", "chat"],
          ["connections", "연결", "link"],
        ]
      : [
          ["chat", "대화", "chat"],
          ["tasks", "작업", "grid"],
          ["connections", "연결", "link"],
        ];
  return `<nav class="bottom-nav" aria-label="주요 메뉴">${items.map(([id, label, icon]) => `<button data-action="screen:${id}" ${state.screen === id ? 'aria-current="page"' : ""}>${i(icon)}<span>${label}</span>${id === "tasks" && pending() ? '<b class="nav-dot" aria-label="승인 대기 있음"></b>' : ""}</button>`).join("")}</nav>`;
}
function offline() {
  return online()
    ? ""
    : `<div class="offline-banner" role="status">${i("wifi")}<span>연결이 끊겼어요. 입력 내용은 유지됩니다.</span>${btn("reconnect", "다시 연결")}</div>`;
}
function approval(compact = false) {
  if (!pending())
    return state.approval === "pending"
      ? ""
      : `<div class="decision-receipt">${i(state.approval === "approved" ? "check" : "pause")} 변경 요청을 ${state.approval === "approved" ? "승인" : "거절"}했습니다 <small>시안</small></div>`;
  return `<section class="approval-card ${compact ? "compact" : ""}" aria-label="승인 대기"><div class="approval-heading"><span class="approval-icon">${i("shield")}</span><div><strong>파일 변경을 기다리고 있어요</strong><small>한국어 UI 점검 · 파일 3개</small></div>${badge("승인 필요", "amber")}</div>${compact ? "" : "<p>버튼 배치와 연결 상태 표시를 개선합니다. 적용할 변경 내용을 먼저 확인해 주세요.</p>"}<div class="approval-actions">${btn("deny", "거절")}${btn("review", "변경 확인", "arrow", "primary")}</div></section>`;
}
function focusApproval() {
  return pending()
    ? `<button class="focus-approval" data-action="review">${i("shield")}<span><strong>파일 3개 변경 · 승인 대기</strong><small>내용을 확인한 뒤 결정해 주세요</small></span>${i("chevron")}</button>`
    : approval(true);
}
function composer() {
  return `<div class="composer-wrap">${concept === "focus" ? focusApproval() : ""}<form class="composer"><label class="sr-only" for="message">메시지</label><textarea id="message" rows="2" placeholder="Hermes에게 요청해 보세요">${esc(state.draft)}</textarea><div class="composer-tools"><div>${ib("attach", "파일 첨부", "plus")}${btn("model", "서버 기본 모델", "down", "model-button")}</div><div>${ib("voice", "음성 입력 시안", "mic")}<button class="send-button" type="submit" aria-label="메시지 보내기" ${online() ? "" : "disabled"}>${i("send")}</button></div></div></form><small class="composer-note">${online() ? "예시 데이터 · 실제 하네스에는 전송되지 않습니다" : "다시 연결한 뒤 메시지를 보낼 수 있습니다"}</small></div>`;
}
function chat() {
  return `<section class="chat-panel"><div class="chat-scroll"><div class="conversation-title"><span>${i("chat")} ${esc(state.session)}</span>${badge("오늘")}</div>${state.newChat ? `<div class="empty-state">${mark()}<p class="eyebrow">YOUR HARNESS, WITH YOU</p><h1>어떤 일을<br>함께 해볼까요?</h1><p>생각을 정리하거나, 진행 중인 작업을 이어가세요.</p>${btn("seed-prompt", "한국어 화면을 점검해 줘", "arrow")}</div>` : `${concept === "paper" ? '<div class="paper-heading"><span>WORK NOTE / 01</span><h1>조금 더 명확한<br>모바일 작업 경험.</h1><p>오늘의 대화 · 한국어 UI 점검</p></div>' : ""}<div class="user-message">모바일 화면의 한국어 문구와 버튼 배치를 점검해 줘.</div><article class="assistant-message"><div class="message-author">${mark()}<strong>Hermes</strong><span>방금</span></div><h2>${concept === "paper" ? "읽는 흐름을 따라, 세 가지 개선." : "한눈에 이해하고,<br>편하게 이어갈 수 있도록."}</h2><p>현재 화면에서 집중을 나누는 요소를 살펴봤어요. 우선 이 세 가지를 정리하면 좋겠습니다.</p><div class="findings"><div><span>01</span><p><strong>연결 상태는 한 곳에</strong>지금 연결된 하네스만 상단에 보여줘요.</p></div><div><span>02</span><p><strong>승인은 행동 가까이에</strong>무엇을 바꾸는지 확인한 뒤 결정해요.</p></div><div><span>03</span><p><strong>펼치면 결과도 함께</strong>대화 옆에서 변경 파일을 살펴봐요.</p></div></div><button class="artifact-card" data-action="open-result">${i("file")}<span><strong>모바일 UI 검토 노트</strong><small>변경 제안 3개 · 초안</small></span>${i("arrow")}</button><div class="message-meta">${i("check")} 화면 검토 완료 <span>·</span> 18초 ${ib("copy", "검토 요약 복사", "copy")}</div></article>${concept === "paper" ? approval(true) : ""}`}${state.messages.map((m) => `<div class="${m.role === "user" ? "user-message" : "local-reply"}">${esc(m.text)}</div>`).join("")}</div>${composer()}</section>`;
}
function tasks() {
  return `<section class="task-panel scroll-panel"><div class="page-kicker">WORKSPACE / OVERVIEW</div><div class="page-heading"><div><h1>지금, 진행 중인 일</h1><p>판단이 필요한 순간을 놓치지 않도록.</p></div>${ib("new-chat", "작업 시작", "plus")}</div><div class="metrics"><div><span>진행 중</span><strong>01<span class="metric-dot"></span></strong></div><div class="attention"><span>승인 대기</span><strong>${pending() ? "01" : "00"}</strong></div><div><span>오늘 완료</span><strong>02</strong></div></div>${approval()}<div class="section-heading"><h2>내 작업</h2>${badge("3개")}</div><div class="task-list">${[
    [
      "한국어 UI 점검",
      pending() ? "변경 검토를 기다리는 중" : "검토 결과 준비됨",
      "shield",
      "amber",
      "review",
    ],
    [
      "연결 안정성 개선",
      "재연결 시나리오 검증 중",
      "activity",
      "",
      "task-detail",
    ],
    ["주간 작업 요약", "완료 · 12분 전", "check", "green", "summary"],
  ]
    .map(
      ([title, sub, icon, color, action], n) =>
        `<button class="task-row" data-action="${action}"><span class="task-icon ${color}">${i(icon)}</span><span class="task-text"><strong>${title}</strong><small>${sub}</small>${n === 1 ? '<span class="progress" role="progressbar" aria-label="연결 안정성 개선 진행률" aria-valuenow="68" aria-valuemin="0" aria-valuemax="100"><span></span></span>' : ""}</span>${i("chevron")}</button>`,
    )
    .join(
      "",
    )}</div><div class="insight-card">${i("spark")}<div><strong>작은 개선이 쌓이고 있어요</strong><p>오늘 완료한 작업과 결정 내용을 한 번에 읽어보세요.</p>${btn("summary", "오늘의 기록", "arrow")}</div></div></section>`;
}
function timeline() {
  return `<ol class="timeline"><li><span class="timeline-dot done"></span><div><strong>요청을 전달했어요</strong><small>10:42 · 한국어 UI 점검</small></div></li><li><span class="timeline-dot done"></span><div><strong>화면 3개를 살펴봤어요</strong><small>10:42 · 대화 / 관리 / 연결</small></div></li><li><span class="timeline-dot ${pending() ? "waiting" : "done"}"></span><div><strong>${pending() ? "변경 승인을 기다리고 있어요" : "검토 결과가 준비됐어요"}</strong><small>10:43 · 변경 파일 3개</small></div></li></ol>`;
}
function context() {
  return `<aside class="context-rail"><div class="section-heading"><h2>${concept === "paper" ? "이 대화의 기록" : "작업 맥락"}</h2>${i("activity")}</div><div class="context-task"><span class="eyebrow">CURRENT TASK</span><h3>한국어 UI 점검</h3><p>더 명확한 화면, 더 편안한 작업 흐름.</p>${badge(pending() ? "승인 대기" : "검토 완료", pending() ? "amber" : "green")}</div><h3 class="small-heading">진행 기록</h3>${timeline()}<h3 class="small-heading">관련 결과</h3><button class="file-row" data-action="open-result">${i("file")}<span>모바일 UI 검토 노트<small>변경 제안 3개</small></span>${i("chevron")}</button><div class="context-footer">${i("shield")} 필요한 권한을 확인하고<br>이번 작업에만 승인합니다.</div></aside>`;
}
function artifact() {
  return `<aside class="artifact-pane"><div class="result-heading"><div>${i("folder")} <strong>작업 결과</strong></div>${badge("미리보기")}</div><div class="result-tabs" role="tablist" aria-label="결과 종류">${[
    ["changes", "변경 내역"],
    ["note", "검토 노트"],
    ["terminal", "실행 기록"],
  ]
    .map(
      ([id, label]) =>
        `<button role="tab" aria-selected="${state.artifact === id}" data-action="artifact:${id}">${label}</button>`,
    )
    .join(
      "",
    )}</div><div class="artifact-scroll" role="tabpanel" aria-label="${state.artifact === "changes" ? "변경 내역" : state.artifact === "note" ? "검토 노트" : "실행 기록"}">${
    state.artifact === "changes"
      ? `<div class="diff-summary"><h2>더 단순한 대화 화면</h2><p>예시 변경 내역 · 파일 3개</p><span class="diff-plus">+24</span> <span class="diff-minus">−18</span></div><div class="diff-file">${i("file")} ChatScreen.kt</div><pre class="code-diff"><code><span class="code-context">  Column {</span>
<span class="code-minus">−   ConnectionDetails()</span>
<span class="code-plus">+   CompactConnectionStatus()</span>
<span class="code-context">    ChatContent()</span>
<span class="code-plus">+   ApprovalBar()</span>
<span class="code-context">    MessageComposer()</span>
<span class="code-context">  }</span></code></pre><div class="changed-file">${i("file")} ConnectionHeader.kt <span>+8 −12</span></div><div class="changed-file">${i("file")} ApprovalBar.kt <span>+14 −0</span></div><p class="muted-note">구조를 비교하기 위한 예시입니다. 실제 저장소 변경 내역이 아닙니다.</p>`
      : state.artifact === "note"
        ? `<article class="note-document"><span class="eyebrow">DESIGN REVIEW</span><h2>모바일 UI 검토 노트</h2><p>연결은 조용하게, 작업은 명확하게.</p><h3>01 · 정보의 우선순위</h3><p>연결 이름과 상태를 상단에 모으고 세부 경로는 눌러서 확인합니다.</p><h3>02 · 확인한 뒤 승인</h3><p>변경 대상과 범위를 함께 표시합니다. 승인은 명시적인 별도 동작으로 유지합니다.</p><h3>03 · 넓어진 만큼 더 보기</h3><p>작업 중인 대화와 결과를 나란히 배치하고, 좁아지면 탭으로 전환합니다.</p></article>`
        : `<div class="terminal"><p><span>10:42:01</span> 요청 수신</p><p><span>10:42:03</span> 화면 구성 확인</p><p><span>10:42:08</span> 한국어 문구 검토</p><p><span>10:42:19</span> 변경 제안 준비</p><p class="terminal-wait">${pending() ? "승인을 기다리고 있습니다…" : "검토를 마쳤습니다."}</p><small>시안용 실행 기록</small></div>`
  }</div><div class="artifact-approval">${approval(true)}</div></aside>`;
}
function connections() {
  return `<section class="connections-panel scroll-panel"><div class="page-kicker">YOUR HARNESS</div><div class="page-heading"><div><h1>연결된 공간</h1><p>내 컴퓨터의 작업을 어디서든 이어가세요.</p></div></div><div class="connection-card"><div class="connection-card-title">${mark()}<div><h2>${esc(state.connection)}</h2><p>${online() ? "Dashboard / Gateway" : "다시 연결이 필요합니다"}</p></div>${badge(online() ? "연결됨" : "오프라인", online() ? "green" : "amber")}</div><div class="capability-list"><span>${i("chat")} 대화</span><span>${i("file")} 파일</span><span>${i("mic")} 음성</span></div><button class="route-row" data-action="connection-details">${i("shield")}<span>Tailscale · HTTPS<small>연결 경로와 사용 권한 보기</small></span>${i("chevron")}</button></div><div class="extension-card"><div>${i("terminal")}<strong>작업 공간 확장하기</strong></div><p>Relay를 페어링하면 터미널과 데스크톱 도구를 함께 사용할 수 있어요.</p>${btn("relay", "Relay 살펴보기", "arrow")}</div>${btn("add-connection", "다른 Hermes 연결", "plus", "wide outlined")}<p class="muted-note">현재 연결과 기능은 모두 시안용 예시 데이터입니다.</p></section>`;
}
function voice() {
  return `<section class="voice-panel"><div class="voice-top"><span class="eyebrow">STAY IN THE FLOW</span><h1>${state.listening ? "듣는 화면을<br>미리 보고 있어요" : "하고 싶은 일을,<br>말로 이어가세요."}</h1><p>${state.listening ? "마이크를 사용하지 않는 시안입니다." : "이동 중에도 내 하네스와 가까이."}</p></div><div class="voice-orbit ${state.listening ? "listening" : ""}" aria-hidden="true"><div></div><div></div><span>${i(state.listening ? "volume" : "mic")}</span></div><div class="voice-bottom"><button class="voice-current" data-action="screen:tasks"><span class="tiny-dot"></span><span><strong>한국어 UI 점검</strong><small>${pending() ? "변경 내용의 승인을 기다려요" : "검토 결과를 확인할 수 있어요"}</small></span>${i("chevron")}</button><div class="thumb-actions">${btn("review", pending() ? "변경 확인" : "검토 결과", "shield", "outlined")}${btn("listen", state.listening ? "시안 멈추기" : "말하기", "mic", "primary")}</div><small class="composer-note">음성 UI 시안 · 실제 녹음이나 전송은 하지 않습니다</small></div></section>`;
}
function render() {
  document.body.className = `theme-${concept}`;
  document.documentElement.style.setProperty("--font-scale", state.font);
  const chatView = state.screen === "chat";
  $("#app").innerHTML =
    `<div class="app-shell ${concept}">${side()}<main class="app-main">${header()}${offline()}${concept === "studio" && chatView ? `<div class="studio-switch" role="tablist" aria-label="작업 패널"><button role="tab" aria-selected="true" data-action="studio-chat">대화</button><button role="tab" aria-selected="false" data-action="studio-result">결과 <span>3</span></button></div>` : ""}<div class="workspace ${chatView ? "chat-workspace" : ""}">${state.screen === "tasks" ? tasks() : state.screen === "connections" ? connections() : state.screen === "voice" ? voice() : chat()}${concept === "studio" && chatView ? artifact() : state.screen === "connections" ? "" : context()}</div>${nav()}</main></div>`;
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
      text: "시안에 메시지를 추가했어요. 실제 하네스에는 전송되지 않습니다.",
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
    "변경 내용을 확인해 주세요",
    `<div class="sheet-callout">${i("shield")}<div><strong>한국어 UI 점검</strong><p>파일 3개 수정 · 이번 작업에만 적용</p></div></div><p>상단 연결 정보를 간결하게 정리하고 입력창 가까이에 승인 요청을 표시하는 예시입니다.</p><ul class="review-files"><li>ChatScreen.kt <span>대화 화면 배치</span></li><li>ConnectionHeader.kt <span>연결 상태 요약</span></li><li>ApprovalBar.kt <span>승인 대기 표시</span></li></ul><p class="muted-note">동작 확인용 시안입니다. 승인해도 실제 파일이나 권한은 변경되지 않습니다.</p>${pending() ? `<div class="sheet-actions">${btn("deny", "거절")}${online() ? btn("approve", "이번 변경 승인", "check", "primary") : btn("reconnect", "다시 연결", "link", "primary")}</div>` : '<p class="decision-receipt">현재 승인 대기 중인 요청이 없습니다.</p>'}`,
  );
}
function resultSheet() {
  sheet(
    "모바일 UI 검토 노트",
    `<div class="note-document"><span class="eyebrow">REVIEW / 01</span><h3>한눈에 이해하고, 편하게 이어가기</h3><p>상단에는 활성 연결과 상태만 남기고, 경로·모델 등 상세 정보는 필요할 때 펼칩니다.</p><h3>변경 전에 확인하기</h3><p>파일 3개의 변경 범위를 확인하고 이번 작업에만 승인합니다.</p><h3>펼친 화면 활용하기</h3><p>대화와 결과를 나란히 표시합니다. 화면 크기가 바뀌어도 작성 중인 메시지는 유지합니다.</p></div>${pending() ? btn("review", "파일 변경 확인", "shield", "primary wide") : ""}`,
  );
}
document.addEventListener("click", (e) => {
  const target = e.target.closest("[data-action]");
  if (!target) return;
  const action = target.dataset.action;
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
        '<div class="note-document"><span class="eyebrow">TODAY / SUMMARY</span><h3>두 가지 일을 마쳤어요.</h3><p>한국어 문구 점검과 화면 구성 검토를 마쳤습니다. 다음으로 연결 상태와 승인 위치를 정리할 차례입니다.</p><p class="muted-note">시안용 예시 기록입니다.</p></div>',
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

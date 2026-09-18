import { icon } from "./icons.js?v=5";
const concepts = [
  {
    id: "focus",
    icon: "chat",
    name: "메시지",
    short: "대화 중심",
    desc: "대화와 첨부 파일, 필요한 승인만 표시합니다.",
    benefit:
      "시간 순서의 대화와 간단한 입력창. 연결·모델 정보는 눌러서 확인합니다.",
    tradeoff: "여러 작업을 보려면 작업 탭으로 이동합니다.",
  },
  {
    id: "mission",
    icon: "list",
    name: "작업 목록",
    short: "미리 알림 구조",
    desc: "승인 대기, 진행 중, 완료된 작업을 목록으로 구분합니다.",
    benefit: "상태별 필터와 짧은 행으로 진행 상황을 확인합니다.",
    tradeoff: "긴 대화는 작업을 선택한 뒤 확인합니다.",
  },
  {
    id: "studio",
    icon: "folder",
    name: "파일",
    short: "대화와 파일 함께",
    desc: "좁은 화면에서는 탭으로, 넓은 화면에서는 나란히 봅니다.",
    benefit:
      "대화와 변경 파일을 함께 확인합니다. 펼치거나 접어도 작성 내용과 선택이 유지됩니다.",
    tradeoff: "좁은 화면에서는 대화와 파일 사이를 전환합니다.",
  },
  {
    id: "paper",
    icon: "book",
    name: "메모",
    short: "문서와 기록 중심",
    desc: "검토 내용, 할 일, 관련 파일을 하나의 문서로 정리합니다.",
    benefit: "제목과 본문, 체크리스트만으로 긴 결과를 읽습니다.",
    tradeoff: "실시간 실행 상태보다 결과를 읽는 데 적합합니다.",
  },
  {
    id: "pulse",
    icon: "mic",
    name: "Javis 음성",
    short: "실시간 음성 비서",
    desc: "음성 상태와 현재 프로필의 작업을 함께 확인합니다.",
    benefit:
      "대기·듣기·확인·응답을 구분합니다. 넓은 화면에서는 대화 기록을 함께 봅니다.",
    tradeoff:
      "실제 녹음·음성 재생 없는 시안입니다. 승인은 검토 화면에서 직접 결정합니다.",
  },
];
const query = new URLSearchParams(location.search);
let current =
  concepts.find((c) => c.id === query.get("concept")) || concepts[0];
const appearance = window.previewAppearance;
let device = query.get("device") === "fold" ? "fold" : "phone";
const $ = (s) => document.querySelector(s);
$("#appearance").value = appearance.getPreference();
$("#directions").innerHTML = concepts
  .map(
    (c, n) =>
      `<button class="direction" data-concept="${c.id}"><span class="direction-icon">${icon(c.icon)}</span><span><strong>${c.name}</strong><small>${c.short}</small></span><span class="n">${n + 1}</span></button>`,
  )
  .join("");
function appUrl() {
  return `app.html?concept=${current.id}&scenario=${$("#scenario").value}&font=${$("#font-size").value}&theme=${window.previewAppearance.getPreference()}&v=5`;
}
function saved() {
  try {
    return localStorage.getItem("hermes-connect-design-choice-v2");
  } catch {
    return null;
  }
}
function render(changeFrame = true) {
  const n = concepts.indexOf(current) + 1;
  $("#concept-index").textContent = `시안 ${n} / 5`;
  $("#concept-title").textContent = current.name;
  $("#concept-desc").textContent = current.desc;
  $("#concept-benefit").textContent = current.benefit;
  $("#concept-tradeoff").textContent = current.tradeoff;
  document
    .querySelectorAll("[data-concept]")
    .forEach((b) =>
      b.setAttribute("aria-current", String(b.dataset.concept === current.id)),
    );
  document.querySelectorAll("[data-device]").forEach((b) => {
    if (b.tagName === "BUTTON")
      b.setAttribute("aria-pressed", String(b.dataset.device === device));
  });
  $(".canvas").dataset.device = device;
  $("#fullscreen").href = appUrl();
  $("#preview").title = `Hermes-Connect ${current.name} 시안`;
  if (changeFrame) $("#preview").src = appUrl();
  const selected = saved() === current.id;
  $("#choose").innerHTML = selected ? "선택한 시안 ✓" : "이 시안 선택";
  $("#selection-status").textContent = selected
    ? `선택한 시안: ${current.name}`
    : "이 브라우저에만 저장됩니다.";
  history.replaceState(
    null,
    "",
    `?concept=${current.id}&device=${device}&theme=${appearance.getPreference()}`,
  );
}
$("#directions").addEventListener("click", (e) => {
  const b = e.target.closest("[data-concept]");
  if (b) {
    current = concepts.find((c) => c.id === b.dataset.concept);
    render();
  }
});
document.querySelectorAll("button[data-device]").forEach((b) =>
  b.addEventListener("click", () => {
    device = b.dataset.device;
    render(false);
  }),
);
for (const id of ["scenario", "font-size"])
  $("#" + id).addEventListener("change", () => {
    $("#preview").contentWindow.postMessage(
      {
        type: "preview-settings",
        scenario: $("#scenario").value,
        font: Number($("#font-size").value),
      },
      location.origin,
    );
    $("#fullscreen").href = appUrl();
  });
$("#appearance").addEventListener("change", () =>
  appearance.setPreference($("#appearance").value),
);
window.addEventListener("appearance-change", () => {
  $("#appearance").value = appearance.getPreference();
  $("#preview").contentWindow.postMessage(
    { type: "preview-appearance", preference: appearance.getPreference() },
    location.origin,
  );
  render(false);
});
$("#choose").addEventListener("click", () => {
  try {
    localStorage.setItem("hermes-connect-design-choice-v2", current.id);
    render(false);
  } catch {
    $("#selection-status").textContent =
      "저장할 수 없습니다. 시안 번호를 기록해 주세요.";
  }
});
window.addEventListener("message", (event) => {
  if (
    event.origin !== location.origin ||
    event.source !== $("#preview").contentWindow
  )
    return;
  if (event.data?.type === "preview-appearance")
    appearance.setPreference(event.data.preference);
  if (event.data?.type === "preview-size")
    $("#viewport-label").textContent =
      `${event.data.width} × ${event.data.height} px`;
});
const dialog = $("#audit-dialog");
$("#audit-button").onclick = () => dialog.showModal();
$("#close-audit").onclick = () => dialog.close();
dialog.addEventListener("click", (e) => {
  if (e.target === dialog) {
    const r = dialog.getBoundingClientRect();
    if (
      e.clientX < r.left ||
      e.clientX > r.right ||
      e.clientY < r.top ||
      e.clientY > r.bottom
    )
      dialog.close();
  }
});
render();

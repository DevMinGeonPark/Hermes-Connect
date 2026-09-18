const concepts = [
  {
    id: "focus",
    en: "FOCUS",
    name: "대화 집중형",
    short: "가장 익숙한 출발점",
    desc: "대화는 넓게. 상태는 짧게. 필요한 순간에만 다음 행동을 보여줍니다.",
    benefit:
      "대화와 입력에 가장 많은 공간을 줍니다. 연결 정보는 한 곳으로 모으고 승인은 입력창 가까이에 둡니다.",
    tradeoff: "여러 작업을 동시에 관찰하려면 작업 탭을 열어야 합니다.",
  },
  {
    id: "mission",
    en: "MISSION",
    name: "작업 관제형",
    short: "진행 상황부터 한눈에",
    desc: "무엇이 진행 중이고, 어디에 내 판단이 필요한지부터 확인합니다.",
    benefit:
      "진행 중인 작업, 승인 대기, 완료된 결과를 첫 화면에서 구분합니다. 멀리서 하네스를 관리할 때 유리합니다.",
    tradeoff:
      "단순한 질문을 시작할 때는 대화 집중형보다 한 단계가 더 필요합니다.",
  },
  {
    id: "studio",
    en: "STUDIO",
    name: "폴더블 작업 공간형",
    short: "대화와 결과를 나란히",
    desc: "접으면 대화 도구. 펼치면 파일·변경 내역까지 함께 보는 작은 작업실.",
    benefit:
      "넓은 화면에서 대화 옆에 결과와 변경 내역을 고정합니다. 화면을 접어도 세션·입력·승인 상태를 유지합니다.",
    tradeoff:
      "좁은 화면에서는 대화와 결과를 탭으로 전환합니다. 정보 밀도가 높아 익숙해지는 시간이 필요합니다.",
  },
  {
    id: "paper",
    en: "PAPER",
    name: "차분한 기록형",
    short: "읽기 좋은 답변과 기록",
    desc: "완료된 결과와 다음 할 일을 읽기 좋은 한 장으로 정리합니다.",
    benefit:
      "긴 답변·요약·결정 기록을 편하게 읽습니다. 파일과 근거를 대화 맥락에 붙여 다시 찾기 쉽게 만듭니다.",
    tradeoff: "동시 실행 상황을 빠르게 훑는 용도에는 관제형이 더 적합합니다.",
  },
  {
    id: "pulse",
    en: "PULSE",
    name: "한 손 조작형",
    short: "보고, 말하고, 승인하기",
    desc: "이동 중에도 하네스 상태를 확인하고, 엄지로 다음 행동을 선택합니다.",
    benefit:
      "자주 쓰는 음성·승인·작업 전환을 아래쪽에 모읍니다. 짧은 확인과 응답에 집중하는 리모컨입니다.",
    tradeoff:
      "긴 코드나 복잡한 결과 검토는 별도 상세 화면으로 이동해야 합니다.",
  },
];
const query = new URLSearchParams(location.search);
let current =
  concepts.find((c) => c.id === query.get("concept")) || concepts[0];
let device = query.get("device") === "fold" ? "fold" : "phone";
const $ = (s) => document.querySelector(s);
$("#directions").innerHTML = concepts
  .map(
    (c, i) =>
      `<button class="direction" data-concept="${c.id}"><span class="n">0${i + 1}</span><span><strong>${c.name}</strong><small>${c.en} / ${c.short}</small></span><span class="arrow">↗</span></button>`,
  )
  .join("");
function appUrl() {
  return `app.html?concept=${current.id}&scenario=${$("#scenario").value}&font=${$("#font-size").value}`;
}
function saved() {
  try {
    return localStorage.getItem("hermes-connect-design-choice");
  } catch {
    return null;
  }
}
function render(changeFrame = true) {
  const n = concepts.indexOf(current) + 1;
  $("#concept-index").textContent = `DIRECTION 0${n} / ${current.en}`;
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
  $("#choose").innerHTML = selected
    ? "선택한 방향 <span>✓</span>"
    : "이 방향 선택 <span>↗</span>";
  $("#selection-status").textContent = selected
    ? `${current.name}이 이 브라우저에 저장되었습니다.`
    : "선택은 이 브라우저에 저장됩니다.";
  history.replaceState(null, "", `?concept=${current.id}&device=${device}`);
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
$("#choose").addEventListener("click", () => {
  try {
    localStorage.setItem("hermes-connect-design-choice", current.id);
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

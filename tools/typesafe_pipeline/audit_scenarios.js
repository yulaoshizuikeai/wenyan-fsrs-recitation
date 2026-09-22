/**
 * TypeSafe-powered GaoKao Scenario Question Quality Audit & Citation Check Pipeline
 * 
 * Implements TypeSafe System One 'Double-checking citations' & 'Verify and escalate' patterns:
 * 1. Deterministic source verification (checking quote against CurriculumDataSource)
 * 2. Multi-dimensional System One semantic evaluation (Jev model: Noul, Choice, Score)
 * 3. Confidence-gated auditing and revision recommendations
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { TypeSafeClient, noul, choice, score } from '@typesafe-ai/sdk';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PROJECT_ROOT = path.resolve(__dirname, '../../');
const SCENARIO_FILE = path.join(PROJECT_ROOT, 'app/src/main/java/com/ancient/wenyan/data/GaoKaoScenarioDataSource.kt');
const CURRICULUM_FILE = path.join(PROJECT_ROOT, 'app/src/main/java/com/ancient/wenyan/data/CurriculumDataSource.kt');

const DEFAULT_API_KEY = process.env.TYPESAFE_API_KEY || 'apikey_2254aa16e56061fe4f9c868636572350faf5_c76c129f715a90ed4d03581f969094d5670dc723133a40701257ecf9a8844e64';

const client = new TypeSafeClient({
  apiKey: DEFAULT_API_KEY
});

// Normalize Chinese text for strict matching (remove punctuation & whitespace)
function sanitizeText(str) {
  if (!str) return '';
  return str.replace(/[\s\p{P}\p{S}]/gu, '');
}

// 1. Parse Articles from CurriculumDataSource.kt
function loadArticles() {
  const content = fs.readFileSync(CURRICULUM_FILE, 'utf-8');
  const articles = new Map(); // id -> { title, author, dynasty, content }

  // Regex to match Article(...) definitions
  const articleRegex = /Article\s*\(\s*"([^"]+)"\s*,\s*"[^"]+"\s*,\s*"([^"]+)"\s*,\s*(?:null|"[^"]*")\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"[^"]+"\s*,\s*(?:true|false)\s*,\s*"[^"]+"\s*,\s*"((?:[^"\\]|\\.)*)"/gs;
  
  let match;
  while ((match = articleRegex.exec(content)) !== null) {
    const id = match[1];
    const title = match[2];
    const author = match[3];
    const dynasty = match[4];
    const rawContent = match[5].replace(/\\n/g, '\n').replace(/\\"/g, '"');

    articles.set(id, { id, title, author, dynasty, content: rawContent });
    // Also index by title for fallback lookup
    articles.set(title, { id, title, author, dynasty, content: rawContent });
  }

  return articles;
}

// 2. Parse GaoKaoScenarioQuestions from GaoKaoScenarioDataSource.kt
function loadQuestions() {
  const content = fs.readFileSync(SCENARIO_FILE, 'utf-8');
  const questions = [];

  // Match GaoKaoScenarioQuestion blocks
  const blockRegex = /GaoKaoScenarioQuestion\s*\(\s*id\s*=\s*"([^"]+)"\s*,\s*articleId\s*=\s*"([^"]+)"\s*,\s*articleTitle\s*=\s*"([^"]+)"\s*,\s*author\s*=\s*"([^"]+)"\s*,\s*prompt\s*=\s*"([^"]+)"\s*,\s*answer\s*=\s*"([^"]+)"\s*,\s*keyPoints\s*=\s*listOf\s*\((.*?)\)\s*,\s*explanation\s*=\s*"([^"]*)"/gs;

  let match;
  while ((match = blockRegex.exec(content)) !== null) {
    const id = match[1];
    const articleId = match[2];
    const articleTitle = match[3];
    const author = match[4];
    const prompt = match[5];
    const answer = match[6];
    const rawKeyPoints = match[7];
    const explanation = match[8];

    const keyPoints = (rawKeyPoints.match(/"([^"]+)"/g) || []).map(s => s.replace(/"/g, ''));

    questions.push({
      id,
      articleId,
      articleTitle,
      author,
      prompt,
      answer,
      keyPoints,
      explanation
    });
  }

  return questions;
}

// 3. Execute Deterministic Citation Match
function verifyCitation(question, article) {
  if (!article) {
    return {
      status: 'ARTICLE_NOT_FOUND',
      found: false,
      snippet: null
    };
  }

  const sanitizedArticle = sanitizeText(article.content);
  const sanitizedAnswer = sanitizeText(question.answer);

  if (sanitizedArticle.includes(sanitizedAnswer)) {
    return {
      status: 'VERIFIED',
      found: true,
      snippet: question.answer
    };
  }

  return {
    status: 'QUOTE_MISMATCH',
    found: false,
    details: `Answer '${question.answer}' not found verbatim in '${article.title}'`
  };
}

// 4. Run TypeSafe System One Semantic Evaluation
async function evaluateWithSystemOne(question, article) {
  const state = {
    article_title: question.articleTitle,
    author: question.author,
    scenario_prompt: question.prompt,
    standard_answer: question.answer,
    scoring_key_points: question.keyPoints,
    full_text_context: article ? article.content.substring(0, 300) : ""
  };

  const response = await client.systemOne({
    state,
    questions: {
      // Noul: Does the prompt accurately target the answer?
      prompt_alignment: noul(
        "Does the `scenario_prompt` accurately and logically direct the student to `standard_answer`?"
      ),

      // Choice: Is the answer uniquely identified by the prompt's cues?
      answer_exclusivity: choice(
        "How uniquely does the scenario prompt point to this specific answer?",
        {
          "uniquely_identified": "The scenario prompt contains specific imagery and logic that uniquely points to this quote.",
          "partially_ambiguous": "The cues are somewhat broad; another sentence in the same article could arguably fit.",
          "too_vague": "The prompt is generic and lacks distinctive constraints."
        }
      ),

      // Score: Gaokao question quality rating on a 0-3 scale
      gaokao_fidelity: score(
        "Rate the prompt against authentic Gaokao classical recitation examination quality:",
        [
          "Flawed phrasing or misleading cues",
          "Acceptable but literal or clunky clues",
          "Authentic Gaokao scenario style and difficulty",
          "Masterful prompt: seamless integration of literary theme and imagery"
        ]
      ),

      // Noul: Are key points valid scoring targets?
      key_points_valid: noul(
        "Are the items in `scoring_key_points` genuinely critical scoring or error-prone focuses (e.g. typos, phonetic loans, key grammatical particles)?"
      )
    }
  });

  return response.answers;
}

// 5. Main Pipeline Runner
async function main() {
  const args = process.argv.slice(2);
  const limitArg = args.find(a => a.startsWith('--limit='));
  const articleArg = args.find(a => a.startsWith('--article='));

  const limit = limitArg ? parseInt(limitArg.split('=')[1], 10) : 4;
  const targetArticle = articleArg ? articleArg.split('=')[1] : null;

  console.log(`=============================================================`);
  console.log(`TypeSafe GaoKao Scenario Question Quality & Citation Pipeline`);
  console.log(`=============================================================`);
  console.log(`Model: jev-latest | Target limit: ${limit} | Filter article: ${targetArticle || 'ALL'}`);

  const articles = loadArticles();
  let questions = loadQuestions();

  console.log(`Loaded ${articles.size / 2} articles from CurriculumDataSource.kt`);
  console.log(`Loaded ${questions.length} questions from GaoKaoScenarioDataSource.kt\n`);

  if (targetArticle) {
    questions = questions.filter(q => q.articleTitle.includes(targetArticle));
  }

  const sample = questions.slice(0, limit);
  const results = [];

  for (let i = 0; i < sample.length; i++) {
    const q = sample[i];
    console.log(`[${i + 1}/${sample.length}] Auditing ${q.id} (${q.articleTitle}) ...`);

    const article = articles.get(q.articleId) || articles.get(q.articleTitle);
    
    // Step 1: Deterministic check
    const citation = verifyCitation(q, article);
    console.log(`  - Citation check: ${citation.status}`);

    // Step 2: Semantic check via TypeSafe
    let sysOne = null;
    try {
      sysOne = await evaluateWithSystemOne(q, article);
      const alignProb = (sysOne.prompt_alignment.noul * 100).toFixed(0);
      const exclusivity = sysOne.answer_exclusivity.choice;
      const fidelityScore = sysOne.gaokao_fidelity.score.toFixed(2);
      const keyPointsProb = (sysOne.key_points_valid.noul * 100).toFixed(0);

      console.log(`  - Prompt Alignment: ${alignProb}% (Noul)`);
      console.log(`  - Exclusivity: ${exclusivity} (Choice, conf: ${(sysOne.answer_exclusivity.confidence * 100).toFixed(0)}%)`);
      console.log(`  - Gaokao Fidelity: ${fidelityScore} / 3.0 (Score, conf: ${(sysOne.gaokao_fidelity.confidence * 100).toFixed(0)}%)`);
      console.log(`  - KeyPoints Validity: ${keyPointsProb}% (Noul)`);

      // Gate: Overall verdict
      let verdict = 'PASSED';
      let issues = [];

      if (!citation.found) {
        verdict = 'FLAGGED';
        issues.push('Citation text mismatch');
      }
      if (sysOne.prompt_alignment.noul < 0.7) {
        verdict = 'FLAGGED';
        issues.push('Low prompt alignment');
      }
      if (exclusivity !== 'uniquely_identified') {
        verdict = 'FLAGGED';
        issues.push(`Exclusivity issue: ${exclusivity}`);
      }
      if (sysOne.gaokao_fidelity.score < 1.5) {
        verdict = 'FLAGGED';
        issues.push('Below Gaokao fidelity threshold');
      }

      console.log(`  - Verdict: ${verdict} ${issues.length ? '(' + issues.join(', ') + ')' : '✔'}\n`);

      results.push({
        question: q,
        citation,
        sysOne,
        verdict,
        issues
      });
    } catch (err) {
      console.error(`  - TypeSafe API error:`, err.message);
    }
  }

  // Generate Markdown Audit Report
  generateReport(results);
}

function generateReport(results) {
  const reportPath = path.join(__dirname, 'audit_report.md');
  let md = `# TypeSafe 高考情境默写题库审计与引文校验报告\n\n`;
  md += `*生成时间: ${new Date().toLocaleString()}*\n`;
  md += `*审计引擎: TypeSafe System One (Jev) + 本地确定性引文校验*\n\n`;

  md += `## 1. 审计概要 (Summary)\n\n`;
  const passedCount = results.filter(r => r.verdict === 'PASSED').length;
  md += `- **抽样题目数**: ${results.length}\n`;
  md += `- **合格通过率**: ${passedCount} / ${results.length} (${((passedCount / results.length) * 100).toFixed(1)}%)\n`;
  md += `- **待优化项**: ${results.length - passedCount}\n\n`;

  md += `| 题号 ID | 篇目 | 引文确定性比对 | 题干指向对齐度 | 排他唯一性 | 高考命题保真分 | 综合结论 |\n`;
  md += `| :--- | :--- | :---: | :---: | :---: | :---: | :---: |\n`;

  for (const r of results) {
    const q = r.question;
    const s = r.sysOne;
    const citeMark = r.citation.found ? '✅ 一致' : '❌ 存疑';
    const align = s ? `${(s.prompt_alignment.noul * 100).toFixed(0)}%` : 'N/A';
    const exclus = s ? s.answer_exclusivity.choice : 'N/A';
    const fid = s ? `${s.gaokao_fidelity.score.toFixed(2)}/3.0` : 'N/A';
    const verdictMark = r.verdict === 'PASSED' ? '✅ **合格**' : `⚠️ **待复核** (${r.issues.join('; ')})`;

    md += `| \`${q.id}\` | ${q.articleTitle} | ${citeMark} | ${align} | \`${exclus}\` | ${fid} | ${verdictMark} |\n`;
  }

  md += `\n## 2. 逐题诊断明细与改进建议\n\n`;

  for (const r of results) {
    const q = r.question;
    const s = r.sysOne;
    md += `### 【${q.id}】《${q.articleTitle}》（${q.author}）\n\n`;
    md += `- **情境设问**: ${q.prompt}\n`;
    md += `- **标准答案**: \`${q.answer}\`\n`;
    md += `- **采分易错点**: ${q.keyPoints.join(' · ')}\n`;
    if (s) {
      md += `- **TypeSafe 概率与置信度详情**:\n`;
      md += `  - 意象对齐度 (Noul): ${(s.prompt_alignment.noul * 100).toFixed(1)}%\n`;
      md += `  - 排他性归因 (Choice): \`${s.answer_exclusivity.choice}\` (置信度: ${(s.answer_exclusivity.confidence * 100).toFixed(1)}%)\n`;
      md += `  - 高考命题水准 (Score): **${s.gaokao_fidelity.score.toFixed(2)} / 3.0** (置信度: ${(s.gaokao_fidelity.confidence * 100).toFixed(1)}%)\n`;
      md += `  - 采分点有效性 (Noul): ${(s.key_points_valid.noul * 100).toFixed(1)}%\n`;
    }
    if (r.issues.length > 0) {
      md += `> [!WARNING]\n> **需要改进的问题**: ${r.issues.join(', ')}\n\n`;
    } else {
      md += `> [!NOTE]\n> **审计结论**: 题干情境逼真，意象与原文答案严丝合缝，采分点精准，符合高考命题标准。\n\n`;
    }
    md += `---\n\n`;
  }

  fs.writeFileSync(reportPath, md, 'utf-8');
  console.log(`Audit report generated at: ${reportPath}`);
}

main().catch(console.error);

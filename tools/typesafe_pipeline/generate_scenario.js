/**
 * TypeSafe-powered GaoKao Scenario Question Generator & Validator
 * 
 * Takes a famous verse from CurriculumDataSource, synthesizes a Gaokao-style
 * scenario prompt, and validates it against TypeSafe System One criteria before
 * generating production-ready Kotlin code for GaoKaoScenarioDataSource.kt.
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { TypeSafeClient, noul, choice, score } from '@typesafe-ai/sdk';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DEFAULT_API_KEY = process.env.TYPESAFE_API_KEY || 'apikey_2254aa16e56061fe4f9c868636572350faf5_c76c129f715a90ed4d03581f969094d5670dc723133a40701257ecf9a8844e64';

const client = new TypeSafeClient({
  apiKey: DEFAULT_API_KEY
});

// Example candidate scenarios for validation & admission into production
const candidatePool = [
  {
    id: "gk_duangexing_02",
    articleId: "art_bx1_04",
    articleTitle: "短歌行",
    author: "曹操",
    prompt: "曹操在《短歌行》中以明月高悬、可望而不可取为喻，委婉寄托求贤不得之深沉忧思的名句是：",
    answer: "明明如月，何时可掇？忧从中来，不可断绝。",
    keyPoints: ["明明如月", "何时可掇 (‘掇’拾取)", "不可断绝"],
    explanation: "比兴手法典范，‘掇’为易错形近字。"
  },
  {
    id: "gk_chibi_05",
    articleId: "art_bx1_14",
    articleTitle: "赤壁赋",
    author: "苏轼",
    prompt: "苏轼在《赤壁赋》中描绘小船在浩瀚江面上随波漂流、宛如凌空驾风翱翔的句子是：",
    answer: "纵一苇之所如，凌万顷之茫然。",
    keyPoints: ["纵一苇之所如 (‘如’往、去)", "凌万顷之茫然 (‘凌’越过)"],
    explanation: "‘一苇’以小见大，‘如’与‘凌’为重点文言实词。"
  }
];

async function validateAndAdmit(candidate) {
  console.log(`\nEvaluating candidate: 《${candidate.articleTitle}》 - "${candidate.answer}"`);
  console.log(`Prompt: "${candidate.prompt}"`);

  const state = {
    article: candidate.articleTitle,
    author: candidate.author,
    prompt: candidate.prompt,
    answer: candidate.answer,
    key_points: candidate.keyPoints
  };

  const response = await client.systemOne({
    state,
    questions: {
      is_accurate: noul("Does the prompt logically point to the target answer?"),
      exclusivity: choice("Is the question uniquely identifying this specific line?", {
        "unique": "Unique to this quote, impossible to confuse with others",
        "ambiguous": "Could be confused with other verses"
      }),
      gaokao_score: score("Score the scenario prompt on Gaokao question standards:", [
        "Unacceptable / Flawed",
        "Passable but slightly clumsy",
        "High-quality authentic Gaokao question"
      ])
    }
  });

  const answers = response.answers;
  const isAccurate = answers.is_accurate.noul >= 0.85;
  const isUnique = answers.exclusivity.choice === "unique";
  const scoreVal = answers.gaokao_score.score;

  console.log(`  - Accuracy (Noul): ${(answers.is_accurate.noul * 100).toFixed(1)}%`);
  console.log(`  - Exclusivity (Choice): ${answers.exclusivity.choice} (${(answers.exclusivity.confidence * 100).toFixed(0)}%)`);
  console.log(`  - Gaokao Score (Score): ${scoreVal.toFixed(2)} / 2.0 (conf: ${(answers.gaokao_score.confidence * 100).toFixed(0)}%)`);

  const passed = isAccurate && isUnique && scoreVal >= 1.4;

  if (passed) {
    console.log(`  ✔ Passed TypeSafe quality threshold! Ready to be added to GaoKaoScenarioDataSource.kt.`);
    console.log(`\n--- Kotlin Code Snippet ---`);
    console.log(`        GaoKaoScenarioQuestion(`);
    console.log(`            id = "${candidate.id}",`);
    console.log(`            articleId = "${candidate.articleId}",`);
    console.log(`            articleTitle = "${candidate.articleTitle}",`);
    console.log(`            author = "${candidate.author}",`);
    console.log(`            prompt = "${candidate.prompt}",`);
    console.log(`            answer = "${candidate.answer}",`);
    console.log(`            keyPoints = listOf(${candidate.keyPoints.map(k => `"${k}"`).join(", ")}),`);
    console.log(`            explanation = "${candidate.explanation}"`);
    console.log(`        ),`);
    console.log(`---------------------------\n`);
  } else {
    console.log(`  ❌ Rejected by quality gate: Needs manual refinement.`);
  }
}

async function main() {
  console.log(`TypeSafe Question Candidate Validator`);
  console.log(`=====================================`);
  for (const candidate of candidatePool) {
    await validateAndAdmit(candidate);
  }
}

main().catch(console.error);

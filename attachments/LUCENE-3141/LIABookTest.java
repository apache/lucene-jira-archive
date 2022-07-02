// $Id$
// $Source$
package com.healthline.hilite;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo.Toffs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

/**
 * TODO: class level javadocs
 * @author Sujit Pal
 * @version $Revision$
 */
public class LIABookTest {

  @Test
  public void testBookExample() throws Exception {
    makeIndex();
    searchIndex();
  }

  private static final String[] TITLES = new String[] {
    "Heart attack",
    "Jaw pain and heart attacks",
    "Heart attack : Definition",
    "Heart attack : Symptoms",
    "Heart attack : Signs and tests",
    "Heart attack : Prevention",
    "Heart attack first aid",
    "Pericarditis - after heart attack",
    "Heart attack : Treatment",
    "Heart attack first aid : Causes",
  };
  private static final String[] CONTENTS = new String[] {
    "  A heart attack is when blood vessels that supply blood to the heart are blocked, preventing enough oxygen from getting to the heart. The heart muscle dies or becomes permanently damaged. Your doctor calls this a myocardial infarction.     Myocardial infarction; MI; Acute MI; ST-elevation myocardial infarction; non-ST-elevation myocardial infarction     Most heart attacks are caused by a blood clot that blocks one of the coronary arteries. The coronary arteries bring blood and oxygen to the heart. If the blood flow is blocked, the heart starves for oxygen and heart cells die.  In  atherosclerosis , plaque builds up in the walls of your coronary arteries. This plaque is made up of cholesterol and other cells. A heart attack can occur as a result of the following:   The slow buildup of plaque may almost block one of your coronary arteries. A heart attack may occur if not enough oxygen-containing blood can flow through this blockage. This is more likely to happen when you are exercising.  The plaque itself develops cracks (fissures) or tears. Blood platelets stick to these tears and form a blood clot (thrombus). A heart attack can occur if this blood clot completely blocks the passage of oxygen-rich blood to the heart.   Occasionally, sudden overwhelming  stress  can trigger a heart attack.  Risk factors for heart attack and coronary artery disease include:   Increasing age (over age 65)  Male gender   Diabetes   Family history of coronary artery disease (genetic or hereditary factors)   High blood pressure    Smoking   Too much  fat  in your diet  Unhealthy cholesterol levels, especially high LDL (\"bad\") cholesterol and low HDL (\"good\") cholesterol  Chronic  kidney disease        Chest pain  is a major symptom of heart attack. You may feel the pain in only one part of your body, or it may move from your chest to your arms, shoulder, neck, teeth, jaw, belly area, or back.  The pain can be severe or mild. It can feel like:   A tight band around the chest  Bad  indigestion   Something heavy sitting on your chest  Squeezing or heavy pressure   The pain usually lasts longer than 20 minutes. Rest and a medicine called nitroglycerin do not completely relieve the pain of a heart attack. Symptoms may also go away and come back.  Other symptoms of a heart attack include:    Anxiety    Cough    Fainting    Light-headedness, dizziness    Nausea  or  vomiting   Palpitations (feeling like your heart is beating too fast)   Shortness of breath    Sweating , which may be extreme   Some people (the elderly, people with diabetes, and women) may have little or no chest pain. Or, they may experience unusual symptoms (shortness of breath, fatigue, weakness). A \"silent heart attack\" is a heart attack with no symptoms.     A heart attack is a medical emergency. If you have symptoms of a heart attack, seek immediate medical help. Call 911 or your local emergency number immediately. DO NOT try to drive yourself to the hospital. DO NOT DELAY, because you are at greatest risk of sudden cardiac death in the early hours of a heart attack.  The health care provider will perform a physical exam and listen to your chest using a stethoscope. The doctor may hear abnormal sounds in your lungs (called crackles), a  heart murmur , or other abnormal sounds.  You may have a  rapid pulse . Your  blood pressure  may be normal, high, or low.  Tests to look at your heart include:    Coronary angiography    CT scan    Echocardiography    Electrocardiogram  (ECG) -- once or repeated over several hours   MRI    Nuclear ventriculography    Blood tests can help show if you have substances produced by heart tissue damage or a high risk for heart attack. These tests include:   Troponin I and troponin T   CPK  and  CPK-MB    Serum myoglobin       If you had a heart attack, you will need to stay in the hospital, possibly in the intensive care unit (ICU). You will be hooked up to an ECG machine, so the health care team can look at how your heart is beating.  Life-threatening irregular heartbeats ( arrhythmias ) are the leading cause of death in the first few hours of a heart attack. These arrythmias may be treated with medications or electrical cardioverson/defibrillation.  The health care team will give you oxygen, even if your blood oxygen levels are normal. This is done so that your body tissues have easy access to oxygen and your heart doesn't have to work as hard.  An intravenous line (IV) will be placed into one of your veins. Medicines and fluids pass through this IV. You may need a tube inserted into your bladder (urinary catheter) so that doctors can see how much fluid your body removes.  ANGIOPLASTY AND STENT PLACEMENT   Angioplasty , also called percutaneous coronary intervention (PCI), is the preferred emergency procedure for opening the arteries for some types of heart attacks. It should preferably be performed within 90 minutes of arriving at the hospital and no later than 12 hours after a heart attack.  Angioplasty is a procedure to open narrowed or blocked blood vessels that supply blood to the heart.  A coronary artery stent is a small, metal mesh tube that opens up (expands) inside a coronary artery. A stent is often placed after angioplasty. It helps prevent the artery from closing up again. A drug eluting stent has medicine in it that helps prevent the artery from closing.  THROMBOLYTIC THERAPY (CLOT-BUSTING DRUGS)  Depending on the results of the ECG, certain patients may be given drugs to break up the clot. It is best if these drugs are given within 3 hours of when the patient first felt the chest pain. This is called thrombolytic therapy. The medicine is first given through an IV. Blood thinners taken by mouth may be prescribed later to prevent clots from forming.  Thrombolytic therapy is not appropriate for people who have:   Bleeding inside their head (intracranial hemorrhage)  Brain abnormalities such as tumors or blood vessel malformations   Stroke  within the past 3 months (or possibly longer)   Head injury  within the past 3 months   Thrombolytic therapy is extremely dangerous in women who are pregnant or in people who have:   A history of using blood thinners such as coumadin  Had major surgery or a major injury within the past 3 weeks  Had internal bleeding within the past 2-4 weeks  Peptic ulcer disease  Severe high blood pressure   OTHER MEDICINES FOR HEART ATTACKS  Many different medicines are used to treat and prevent heart attacks. Nitroglycerin helps reduce chest pain. You may also receive strong medicines to relieve pain.  Antiplatelet medicines help prevent clots from forming. Aspirin is an antiplatelet drug. Another one is clopidogrel (Plavix). Ask your doctor which of these drugs you should be taking. Always talk to your health care provider before stopping either of these drugs.   For the first year after a heart attack, you will likely take both aspirin and clopidogrel every day. After that, your health care provider may only prescribe aspirin.  If you had angioplasty and a coronary stent placed after your heart attack, you may need to take clopidogrel with your aspirin for longer than one year.   Other medications you may receive during or after a heart attack include:   Beta-blockers (such as metoprolol, atenolol, and propranolol) help reduce the strain on the heart and lower blood pressure.  ACE inhibitors (such as ramipril, lisinopril, enalapril, or captopril) are used to prevent heart failure and lower blood pressure.  Lipid-lowering medications, especially statins (such as lovastatin, pravastatin, simvastatin, atorvastatin, and rosuvastatin) reduce blood cholesterol levels to prevent plaque from increasing. They may reduce the risk of another heart attack or death.   CORONARY ARTERY BYPASS SURGERY  Coronary angiography may reveal severe coronary artery disease in many vessels, or a narrowing of the left main coronary artery (the vessel supplying most of the blood to the heart). In these circumstances, the cardiologist may recommend emergency coronary artery bypass surgery ( CABG ). This procedure is also called \"open heart surgery.\" The surgeon takes either a vein or artery from another location in your body and uses it to bypass the blocked coronary artery.     See: Heart disease -- resources      How well you do after a heart attack depends on the amount and location of damaged tissue. Your outcome is worse if the heart attack caused damage to the signaling system that tells the heart to contract.  About a third of heart attacks are deadly. If you live 2 hours after an attack, you are likely to survive, but you may have complications. Those who do not have complications may fully recover.  Usually a person who has had a heart attack can slowly go back to normal activities, including sexual activity.       Cardiogenic shock    Congestive heart failure   Damage extending past heart tissue (infarct extension), possibly leading to rupture of the heart  Damage to heart valves or the wall between the two sides of the heart  Inflammation around the lining of the heart ( pericarditis )  Irregular heartbeats, including  ventricular tachycardia  and ventricular fibrillation  Blood clot in the lungs (pulmonary embolism)  Blood clot to the brain ( stroke )  Side effects of drug treatment      Immediately call your local emergency number (such as 911) if you have symptoms of a heart attack.     To prevent a heart attack:   Keep your blood pressure, blood sugar, and cholesterol under control.  Don't smoke.  Consider drinking 1 to 2 glasses of alcohol or wine each day. Moderate amounts of alcohol may reduce your risk of cardiovascular problems. However, drinking larger amounts does more harm than good.  Eat a low-fat diet rich in fruits and vegetables and low in animal fat.  Eat fish twice a week. Baked or grilled fish is better than fried fish. Frying can destroy some of the health benefits.  Exercise daily or several times a week. Walking is a good form of exercise. Talk to your doctor before starting an exercise routine.  Lose weight if you are overweight.   If you have one or more risk factors for heart disease, talk to your doctor about possibly taking aspirin to help prevent a heart attack. Aspirin therapy (75 mg to 325 mg a day) or a drug called clopidogrel may be prescribed for women at high risk for heart disease.  Aspirin therapy is recommended for women over age 65 to prevent a heart attack and stroke. However, it is only recommended if blood pressure is controlled and the benefit is likely to outweigh the risk of gastrointestinal side effects. Regular use of aspirin is not recommended for healthy women under age 65 to prevent heart attacks.  New guidelines no longer recommend hormone replacement therapy, vitamins E or C, antioxidants, or folic acid to prevent heart disease.  After a heart attack, you will need regular follow-up care to reduce the risk of having a second heart attack. Often, a cardiac rehabilitation program is recommended to help you gradually return to a normal lifestyle. Always follow the exercise, diet, and medication plan prescribed by your doctor.                                Anderson JL, Adams CD, Antman EM, Bridges CR, Califf RM, Casey DE Jr., et al. ACC/AHA 2007 guidelines for the management of patients with unstable angina/non-ST-elevation myocardial infarction: a report of the American College of Cardiology/American Heart Association Task Force on Practice Guidelines (Writing Committee to Revise the 2002 Guidelines for the Management of Patients With Unstable Angina/Non-ST-Elevation Myocardial Infarction) developed in collaboration with the American College of Emergency Physicians, the Society for Cardiovascular Angiography and Interventions, and the Society of Thoracic Surgeons endorsed by the American Association of Cardiovascular and Pulmonary Rehabilitation and the Society for Academic Emergency Medicine.  J Am coll Cardiol . 2007;50:e1-e157.  King SB 3rd, Smith SC Jr., Hirschfeld JW Jr., Jacobs AK, Morrison DA, Williams DO, et al. 2007 Focused Update of the ACC/AHA/SCAI 2005 Guideline Update for Percutaneous Coronary Intervention: a report of the American College of Cardiology/American Heart Association Task Force on Practice Guidelines: 2007 Writing Group to Review New Evidence and Update the ACC/AHA/SCAI 2005 Guideline Update for Percutaneous Coronary Intervention. Writing on Behalf of the 2005 Writing Committee.  Circulation . 2008;117:261-295.  Antman Em. ST-Elevation myocardial infarction: managemtn. In: Libby P, Bonow RO, Mann DL, Zipes DP, eds.  Braunwald's Heart Disease: A Textbook of Cardiovascular Medicine . 8th ed. Philadelphia, Pa: Saunders Elsever; 2007:chap 51.  Goodman SG, Menon V, Cannon CP, Steg G, Ohman EM, Harrington RA, et al. Acute ST-segment elevation myocardial infarction: American College of Chest Physicians Evidence-Based Clinical Practice Guidelines (8th edition).  Chest . 2008;133:708S-775S." ,
    "Tooth pain and heart attacks; Heart attacks and jaw pain            Question:  Can pain in the jaw or teeth be an indication of a  heart attack ?  Answer:  Sometimes. Heart pain can radiate to the jaw and teeth. It is more common for heart-related discomfort to affect the lower jaw than the upper jaw. It cannot be emphasized enough that a heart attack can have symptoms other than chest pain and these symptoms should be checked immediately.  Pain in the upper teeth also can indicate other conditions, such as a  sinus infection . It's important to get evaluated by your doctor to know the cause of your symptoms.  See also:    Acute MI    Chest pain            ", 
    "A heart attack is when blood vessels that supply blood to the heart are blocked, preventing enough oxygen from getting to the heart. The heart muscle dies or becomes permanently damaged. Your doctor calls this a myocardial infarction. ", 
    "Chest pain  is a major symptom of heart attack. You may feel the pain in only one part of your body, or it may move from your chest to your arms, shoulder, neck, teeth, jaw, belly area, or back.  The pain can be severe or mild. It can feel like:   A tight band around the chest  Bad  indigestion   Something heavy sitting on your chest  Squeezing or heavy pressure   The pain usually lasts longer than 20 minutes. Rest and a medicine called nitroglycerin do not completely relieve the pain of a heart attack. Symptoms may also go away and come back.  Other symptoms of a heart attack include:    Anxiety    Cough    Fainting    Light-headedness, dizziness    Nausea  or  vomiting   Palpitations (feeling like your heart is beating too fast)   Shortness of breath    Sweating , which may be extreme   Some people (the elderly, people with diabetes, and women) may have little or no chest pain. Or, they may experience unusual symptoms (shortness of breath, fatigue, weakness). A \"silent heart attack\" is a heart attack with no symptoms.  ",
    "  A heart attack is a medical emergency. If you have symptoms of a heart attack, seek immediate medical help. Call 911 or your local emergency number immediately. DO NOT try to drive yourself to the hospital. DO NOT DELAY, because you are at greatest risk of sudden cardiac death in the early hours of a heart attack.  The health care provider will perform a physical exam and listen to your chest using a stethoscope. The doctor may hear abnormal sounds in your lungs (called crackles), a  heart murmur , or other abnormal sounds.  You may have a  rapid pulse . Your  blood pressure  may be normal, high, or low.  Tests to look at your heart include:    Coronary angiography    CT scan    Echocardiography    Electrocardiogram  (ECG) -- once or repeated over several hours   MRI    Nuclear ventriculography    Blood tests can help show if you have substances produced by heart tissue damage or a high risk for heart attack. These tests include:   Troponin I and troponin T   CPK  and  CPK-MB    Serum myoglobin   ", 
    "  To prevent a heart attack:   Keep your blood pressure, blood sugar, and cholesterol under control.  Don't smoke.  Consider drinking 1 to 2 glasses of alcohol or wine each day. Moderate amounts of alcohol may reduce your risk of cardiovascular problems. However, drinking larger amounts does more harm than good.  Eat a low-fat diet rich in fruits and vegetables and low in animal fat.  Eat fish twice a week. Baked or grilled fish is better than fried fish. Frying can destroy some of the health benefits.  Exercise daily or several times a week. Walking is a good form of exercise. Talk to your doctor before starting an exercise routine.  Lose weight if you are overweight.   If you have one or more risk factors for heart disease, talk to your doctor about possibly taking aspirin to help prevent a heart attack. Aspirin therapy (75 mg to 325 mg a day) or a drug called clopidogrel may be prescribed for women at high risk for heart disease.  Aspirin therapy is recommended for women over age 65 to prevent a heart attack and stroke. However, it is only recommended if blood pressure is controlled and the benefit is likely to outweigh the risk of gastrointestinal side effects. Regular use of aspirin is not recommended for healthy women under age 65 to prevent heart attacks.  New guidelines no longer recommend hormone replacement therapy, vitamins E or C, antioxidants, or folic acid to prevent heart disease.  After a heart attack, you will need regular follow-up care to reduce the risk of having a second heart attack. Often, a cardiac rehabilitation program is recommended to help you gradually return to a normal lifestyle. Always follow the exercise, diet, and medication plan prescribed by your doctor.  ",
    "  A  heart attack  is a medical emergency.  The average person waits 3 hours before seeking help for symptoms of a heart attack. Many heart attack victims die before they reach a hospital. The sooner someone gets to the emergency room, the better the chance of survival. Prompt medical treatment also reduces the amount of damage done to the heart following an attack.     First aid - heart attack; First aid - cardiopulmonary arrest; First aid - cardiac arrest      Heart disease is the leading cause of death in America today.     A heart attack occurs when the blood flow that carries oxygen to the heart is blocked. The heart muscle becomes starved for oxygen and begins to die. SeeÊ heart attack  for more specific causes.     Heart attacks can cause a wide range of symptoms, from mild to intense. Women, the elderly, and people with diabetes are more likely to have subtle or atypical symptoms.  Symptoms in adults may include:    Chest pain   Usually in the center of the chest  Lasts for more than a few minutes or comes and goes  May feel like pressure, squeezing, fullness  Pain may be felt in other areas of the upper body, such as the jaw, shoulder, one or both arms, back, and stomach area    Cold sweat  Lightheadedness  Nausea   Shortness of breath    Women are more likely than men to have symptoms of nausea, vomiting, back or jaw pain, and shortness of breath with chest pain.  Babies and children may appear limp and unresponsive and may have bluish-colored skin.      Have the person sit down, rest, and try to keep calm.  Loosen any tight clothing.  Ask if the person takes any chest pain medication for aÊknown heart condition.  Help the person take the medication (usually nitroglycerin, which is placed under the tongue).  If the pain does not go away promptly with rest or within 3 minutes of taking nitroglycerin, call for emergency medical help.  If the person is  unconscious  and unresponsive, call 911 (or your local emergency number), then begin  CPR .  If an infant or child is unconscious and unresponsive, perform 1 minute of CPR, then call 911.       Do NOT leave the person alone except to call for help, if necessary.  Do NOT allow the person to deny the symptoms and convince you not to call for emergency help.  Do NOT wait to see if the symptoms go away.  Do NOT give the person anything by mouth unless a heart medication (such as nitroglycerin) has been prescribed.       If sudden chest pain or other symptoms of a heart attack occur.  If an adult or child is unresponsive orÊis notÊbreathing.      Adults should take steps to control heart disease risk factors whenever possible. If you smoke, quit. Smoking more than doubles the chance of developing  heart disease . Keep  blood pressure , cholesterol, and  diabetes  in good control andÊfollow with your doctor'sÊorders.  Lose weight if  obese  or overweight. GetÊregular exercise to improve heart health. (Talk to your doctorÊbefore starting any new fitness program.)  Limit the amount of alcohol you drink.ÊOne drink a day is associated with reducing the rate of heart attacks, butÊtwo or more drinks a day can damage the heart and cause other medical problems.              ",
    "  Pericarditis is inflammation and swelling of the covering of the heart (pericardium). The condition can occur in the days or weeks following a heart attack.  See also:  Bacterial pericarditis      Dressler syndrome; Post-MI pericarditis; Post-cardiac injury syndrome; Postcardiotomy pericarditis     Pericarditis may occur within 2 to 5 days after a  heart attack , or it may occur as much as 11 weeks later. The condition is called Dressler\\'s syndrome when it persists for weeks or months after a heart attack.  Pericarditis that occurs shortly after a heart attack is caused by an overactive response by the body\\'s immune system. When the body senses blood in the pericardial sac or dead or severely damaged heart tissue (as with a heart attack), it triggers an  inflammatory response . Cells from the immune system try to clean up the heart after injury, but, in some cases, the cells can attack healthy tissue by mistake.  Pain occurs when the pericardium becomes inflamed (swollen) and rubs on the heart.  You have a higher risk of pericarditis if you have had a previous heart attack, open heart surgery, or chest trauma.       Anxiety    Chest pain   May come and go (recur)  Pain may be sharp and stabbing (pleuritic) or tight and crushing (ischemic)  Pain may get worse when breathing and may be go away when you stand or sit up  Pain moves to the neck, shoulder, back, or abdomen     Difficulty breathing   Dry  cough    Fast heart rate  (tachycardia)   Fatigue    Fever    General ill feeling  (malaise)  Splinting of ribs (bending over or holding the chest) with deep breathing      The health care provider will use a stethoscope to listen to the heart and lungs. There may be a rubbing sound (not to be confused with a murmur), and  heart sounds  in general may be weak or sound far away.  Buildup of fluid in the covering of the heart or space around the lungs ( pleural effusion ) is not common after heart attack. But, it does occur in some patients with Dressler\\'s syndrome.  Tests may include:   Cardiac markers (CK-MB and troponin may help distinguish  pericarditis  from a heart attack)   Chest CT scan    Chest MRI    Chest x-ray    Complete blood count  shows increased  white blood cells    ECG    Echocardiogram    ESR  (sedimentation rate) is high      The goal of treatment is to make the heart work better and reduce pain and other symptoms.  Nonsteroidal anti-inflammatory medications (NSAIDs) and aspirin may be used to treat inflammation of the pericardium. In extreme cases, when other medicines don't work, steroids or colchicine may be used.  In some cases, excess fluid surrounding the heart may need to be removed. This is done with a procedure called pericardiocentesis . If complications develop, part of the pericardium may need to be removed with surgery (pericardiectomy).         The condition may come back even in those who receive treatment. However, untreated pericarditis can be life threatening.       Cardiac tamponade   Constrictive  heart failure    Pulmonary edema       Call your health care provider if you develop symptoms of pericarditis following a heart attack.  Call your health care provider if pericarditis has been diagnosed and symptoms continue or come back, despite treatment.                    LeWinter MM. Pericardial Diseases. In: Libby P, Bonow RO, Mann DL, Zipes DP, eds. Braunwald's Heart Disease: A Textbook of Cardiovascular Medicine. 8th ed. Philadelphia, Pa; Saunders Elsevier; 2007: chap 70. ", 
    "  If you had a heart attack, you will need to stay in the hospital, possibly in the intensive care unit (ICU). You will be hooked up to an ECG machine, so the health care team can look at how your heart is beating.  Life-threatening irregular heartbeats ( arrhythmias ) are the leading cause of death in the first few hours of a heart attack. These arrythmias may be treated with medications or electrical cardioverson/defibrillation.  The health care team will give you oxygen, even if your blood oxygen levels are normal. This is done so that your body tissues have easy access to oxygen and your heart doesn't have to work as hard.  An intravenous line (IV) will be placed into one of your veins. Medicines and fluids pass through this IV. You may need a tube inserted into your bladder (urinary catheter) so that doctors can see how much fluid your body removes.  ANGIOPLASTY AND STENT PLACEMENT   Angioplasty , also called percutaneous coronary intervention (PCI), is the preferred emergency procedure for opening the arteries for some types of heart attacks. It should preferably be performed within 90 minutes of arriving at the hospital and no later than 12 hours after a heart attack.  Angioplasty is a procedure to open narrowed or blocked blood vessels that supply blood to the heart.  A coronary artery stent is a small, metal mesh tube that opens up (expands) inside a coronary artery. A stent is often placed after angioplasty. It helps prevent the artery from closing up again. A drug eluting stent has medicine in it that helps prevent the artery from closing.  THROMBOLYTIC THERAPY (CLOT-BUSTING DRUGS)  Depending on the results of the ECG, certain patients may be given drugs to break up the clot. It is best if these drugs are given within 3 hours of when the patient first felt the chest pain. This is called thrombolytic therapy. The medicine is first given through an IV. Blood thinners taken by mouth may be prescribed later to prevent clots from forming.  Thrombolytic therapy is not appropriate for people who have:   Bleeding inside their head (intracranial hemorrhage)  Brain abnormalities such as tumors or blood vessel malformations   Stroke  within the past 3 months (or possibly longer)   Head injury  within the past 3 months   Thrombolytic therapy is extremely dangerous in women who are pregnant or in people who have:   A history of using blood thinners such as coumadin  Had major surgery or a major injury within the past 3 weeks  Had internal bleeding within the past 2-4 weeks  Peptic ulcer disease  Severe high blood pressure   OTHER MEDICINES FOR HEART ATTACKS  Many different medicines are used to treat and prevent heart attacks. Nitroglycerin helps reduce chest pain. You may also receive strong medicines to relieve pain.  Antiplatelet medicines help prevent clots from forming. Aspirin is an antiplatelet drug. Another one is clopidogrel (Plavix). Ask your doctor which of these drugs you should be taking. Always talk to your health care provider before stopping either of these drugs.   For the first year after a heart attack, you will likely take both aspirin and clopidogrel every day. After that, your health care provider may only prescribe aspirin.  If you had angioplasty and a coronary stent placed after your heart attack, you may need to take clopidogrel with your aspirin for longer than one year.   Other medications you may receive during or after a heart attack include:   Beta-blockers (such as metoprolol, atenolol, and propranolol) help reduce the strain on the heart and lower blood pressure.  ACE inhibitors (such as ramipril, lisinopril, enalapril, or captopril) are used to prevent heart failure and lower blood pressure.  Lipid-lowering medications, especially statins (such as lovastatin, pravastatin, simvastatin, atorvastatin, and rosuvastatin) reduce blood cholesterol levels to prevent plaque from increasing. They may reduce the risk of another heart attack or death.   CORONARY ARTERY BYPASS SURGERY  Coronary angiography may reveal severe coronary artery disease in many vessels, or a narrowing of the left main coronary artery (the vessel supplying most of the blood to the heart). In these circumstances, the cardiologist may recommend emergency coronary artery bypass surgery ( CABG ). This procedure is also called \"open heart surgery.\" The surgeon takes either a vein or artery from another location in your body and uses it to bypass the blocked coronary artery. ", 
    "  A heart attack occurs when the blood flow that carries oxygen to the heart is blocked. The heart muscle becomes starved for oxygen and begins to die. SeeÊ heart attack  for more specific causes.  ",
  };
  
  // configuration
  private static final String PRE_TAG = "<span style=\"background-color:#ffff00\">";
  private static final String POST_TAG = "</span>";
  private static final int CONTENT_FRAG_LEN = 250;
  private static final int TITLE_FRAG_LEN = -1;
  
  private static Directory DIR = new RAMDirectory();
  
  private void makeIndex() throws IOException {
    IndexWriterConfig iwconf = new IndexWriterConfig(Version.LUCENE_30, getAnalyzer());
    IndexWriter writer = new IndexWriter(DIR, iwconf);
    int ndocs = TITLES.length;
    for (int i = 0; i < ndocs; i++) {
      Document doc = new Document();
      doc.add(new Field("title", TITLES[i], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
      doc.add(new Field("content", CONTENTS[i], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
      writer.addDocument(doc);
    }
    writer.close();
  }

  private static final String[] FIELDS = new String[] {"title", "content"};
  
  private void searchIndex() throws Exception {
    IndexSearcher searcher = new IndexSearcher(DIR);
    for (String field : FIELDS) {
      QueryParser parser = new QueryParser(Version.LUCENE_30, field, getAnalyzer());
      Query q = parser.parse(field + ":\"heart attack\"");
      int fragLen = "content".equals(field) ? CONTENT_FRAG_LEN : TITLE_FRAG_LEN; 
      FastVectorHighlighter highlighter = getHighlighter(fragLen);
      FieldQuery fq = highlighter.getFieldQuery(q);
      ScoreDoc[] hits = searcher.search(q, TITLES.length).scoreDocs;
      for (ScoreDoc hit : hits) {
        String snippet = highlighter.getBestFragment(
          fq, searcher.getIndexReader(), hit.doc, field, CONTENT_FRAG_LEN);
        System.out.println(field + "=[" + snippet +
          ("content".equals(field) ? "..." : "") + "]");
      }
    }
    searcher.close();
  }

  @SuppressWarnings("unchecked")
  private Analyzer getAnalyzer() throws IOException {
    final Set<String> stopset = new HashSet<String>();
    List<String> lines = FileUtils.readLines(new File("/Users/sujit/Projects/solr-svn-3.1/solr/example/solr/conf/stopwords.txt"));
    for (String line : lines) {
      if (StringUtils.isEmpty(line) || line.startsWith("#")) {
        continue;
      }
      stopset.add(StringUtils.trim(line));
    }
    return new Analyzer() {
      @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        Tokenizer tokenizer = new StandardTokenizer(Version.LUCENE_30, reader);
        TokenFilter filter = new LowerCaseFilter(Version.LUCENE_31, tokenizer);
        filter = new StopFilter(Version.LUCENE_30, filter, stopset); 
        filter = new PorterStemFilter(filter);
        return filter;
      }
    };
  }

  private FastVectorHighlighter getHighlighter(int fragLen) {
    FragListBuilder fragListBuilder = new SimpleFragListBuilder();
    FragmentsBuilder fragBuilder = new MyFragmentsBuilder(
      PRE_TAG, POST_TAG, fragLen);
    return new FastVectorHighlighter(true, true, 
      fragListBuilder, fragBuilder);
  }

  private class MyFragmentsBuilder extends SimpleFragmentsBuilder {

    private int fragLen;
    private String pretag;
    private String posttag;
    
    public MyFragmentsBuilder(String pretag, String posttag, int fragLen) {
      super(new String[] {pretag}, new String[] {posttag});
      this.pretag = pretag;
      this.posttag = posttag;
      this.fragLen = fragLen;
    }
    
    @Override
    public String createFragment(IndexReader reader, int docId,
        String fieldName, FieldFragList fieldFragList ) 
        throws IOException {
      // read the source string back from the index
      Document doc = reader.document(docId);
      String source = doc.get(fieldName);
      if (StringUtils.isEmpty(source)) {
        return source;
      }
      // find the first occurrence of the matched phrase
      List<Range> termPositions = new ArrayList<Range>();
      List<WeightedFragInfo> fragInfos = fieldFragList.getFragInfos();
      for (WeightedFragInfo fragInfo : fragInfos) {
        List<SubInfo> subInfos = fragInfo.getSubInfos();
        for (SubInfo subInfo : subInfos) {
          List<Toffs> termOffsets = subInfo.getTermsOffsets();
          for (Toffs termOffset : termOffsets) {
            Range termPosition = new IntRange(
              termOffset.getStartOffset(), termOffset.getEndOffset());
            termPositions.add(termPosition);
          }
        }
      }
      if (termPositions.size() == 0) {
        return StringUtils.substring(source, 0, fragLen);
      }
      int startFragPosition = 0;
      int endFragPosition = 0;
      // read back on the char array until we find a period,
      // then read front until we find a letter/digit. This
      // is our fragment start position. If no period found,
      // then this must be the first sentence, start here.
      if (fragLen < 0) {
        // we don't need a fragLen for titles, take them whole
        // so in this case fragLen should be -1.
        endFragPosition = source.length();
      } else {
        int startTermPosition = termPositions.get(0).getMinimumInteger();
        char[] sourceChars = source.toCharArray();
        for (int i = startTermPosition; i >= 0; i--) {
          if (sourceChars[i] == '.') {
            startFragPosition = i;
            break;
          }
        }
        for (int i = startFragPosition; i < sourceChars.length; i++) {
          if (Character.isLetterOrDigit(sourceChars[i])) {
            startFragPosition = i;
            break;
          }
        }
        endFragPosition = Math.min(startFragPosition + fragLen, sourceChars.length);
      }
      // return the substring bounded by start and end, highlighting
      // the matched phrase
      final Range fragRange = 
        new IntRange(startFragPosition, endFragPosition);
      CollectionUtils.filter(termPositions, new Predicate() {
        public boolean evaluate(Object obj) {
          Range r = (Range) obj;
          return (fragRange.containsRange(r));
        }
      });
      if (termPositions.size() == 0) {
        // unlikely, since we are pretty sure that there is at least
        // one term position in our fragRange, but just being paranoid
        return StringUtils.substring(source, startFragPosition, endFragPosition);
      }
      StringBuilder buf = new StringBuilder();
      buf.append(StringUtils.substring(
        source, startFragPosition, 
        termPositions.get(0).getMinimumInteger()));
      int numHighlights = termPositions.size();
      for (int i = 0; i < numHighlights; i++) {
        buf.append(pretag).
          append(StringUtils.substring(source, 
            termPositions.get(i).getMinimumInteger(), 
            termPositions.get(i).getMaximumInteger())).
          append(posttag);
        if (i < numHighlights - 1) {
          buf.append(StringUtils.substring(source, 
            termPositions.get(i).getMaximumInteger(), 
            termPositions.get(i+1).getMinimumInteger()));
        }
      }
      buf.append(StringUtils.substring(source, 
        termPositions.get(numHighlights-1).getMaximumInteger(), 
        fragRange.getMaximumInteger())); 
      return buf.toString();
    }    
  }
}

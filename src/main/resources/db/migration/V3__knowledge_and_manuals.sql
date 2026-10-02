-- V3 extends public content through new releases; V1/V2 remain unchanged.
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','afdac469-0fe4-5007-833c-51a71333967b',2,'PUBLISHED',UTC_TIMESTAMP(6),'0ba4f28322c4e35ffc9ea0a4c0fbf44442122910df421855f0c7ab94866aa2ff');
INSERT INTO chapter_revision SELECT 'db3612d9-efd1-592c-af86-104ffa76daba',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='2a075740-9067-533f-823b-f721b3a73140';
INSERT INTO item_revision SELECT 'db3612d9-efd1-592c-af86-104ffa76daba',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='2a075740-9067-533f-823b-f721b3a73140';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('92586ac9-22a1-55f4-bc26-e08aea985187','afdac469-0fe4-5007-833c-51a71333967b','limits');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187','极限与连续','核心：lim f(x)=A 表示 x 趋近指定点时，函数值趋近 A。连续要求 lim[x→a] f(x)=f(a)。
常用极限（弧度制）：lim[x→0] sin x / x = 1；lim[x→0] (eˣ−1)/x = 1；lim[x→0] ln(1+x)/x = 1；lim[x→0] (1−cos x)/x² = 1/2。
等价无穷小：x→0 时，sin x ~ x，tan x ~ x，ln(1+x) ~ x，eˣ−1 ~ x，1−cos x ~ x²/2。
易错：等价替换适合乘除结构，不能在加减式中随意逐项替换；三角函数求导默认弧度。',2,'[{"label":"基本极限","tex":"\\\\lim_{x\\\\to0}\\\\frac{\\\\sin x}{x}=1","condition":"三角函数采用弧度制"},{"label":"指数与对数","tex":"\\\\lim_{x\\\\to0}\\\\frac{e^x-1}{x}=\\\\lim_{x\\\\to0}\\\\frac{\\\\ln(1+x)}{x}=1","condition":"在定义域内趋近 0"},{"label":"常用展开","tex":"e^x=1+x+\\\\frac{x^2}{2}+\\\\frac{x^3}{6}+o(x^3)","condition":"x→0"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('4717ac57-bbe5-5b37-aba7-672735d213d8','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',1,'求 $\\lim_{x\\to0}\\sin(2x)/x$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('4717ac57-bbe5-5b37-aba7-672735d213d8','$2$','写成 $2\\sin(2x)/(2x)$，使用基本极限。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a8290c58-244d-5c5d-9190-7003ac379d6b','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',2,'求 $\\lim_{x\\to0}(1-\\cos x)/x^2$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a8290c58-244d-5c5d-9190-7003ac379d6b','$1/2$','由 $1-\\cos x=2\\sin^2(x/2)$ 化为基本极限。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('76782f35-d767-54ee-9154-9ca4fec7f71e','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',3,'求 $\\lim_{x\\to0}(e^x-1-x)/x^2$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('76782f35-d767-54ee-9154-9ca4fec7f71e','$1/2$','$e^x=1+x+x^2/2+o(x^2)$，分子消去前两项。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7450eeef-e468-5205-8258-a71691564b9f','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',4,'求 $\\lim_{x\\to0}(\\ln(1+x)-x+x^2/2)/x^3$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7450eeef-e468-5205-8258-a71691564b9f','$1/3$','对数展开到三阶：$\\ln(1+x)=x-x^2/2+x^3/3+o(x^3)$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b8da2c0d-bdc5-5435-a9ab-3abb536b295b','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',5,'求常数 $a,b$ 使 $\\lim_{x\\to0}\\frac{e^{\\sin x}-1-ax-bx^2}{x^3}$ 有限，并求极限。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b8da2c0d-bdc5-5435-a9ab-3abb536b295b','$a=1,\\ b=1/2$；极限为 $0$。','先代入 $\\sin x=x-x^3/6+o(x^3)$，再展开指数，得到 $e^{\\sin x}=1+x+x^2/2+o(x^3)$。有限性要求一、二阶项均消去；三阶系数恰为 0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ea29eef8-3eb9-50df-9b15-24e59fece07c','db3612d9-efd1-592c-af86-104ffa76daba','92586ac9-22a1-55f4-bc26-e08aea985187',2,'极限与连续 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ea29eef8-3eb9-50df-9b15-24e59fece07c','例：lim[x→0] sin(3x)/x = 3。','例：lim[x→0] sin(3x)/x = 3。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('467c4693-6e97-5813-9a7f-df24ec6546a9','afdac469-0fe4-5007-833c-51a71333967b','derivatives');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9','基本求导公式表','设 C 为常数。以下公式均在函数定义域内且可导的位置使用。
(C)′ = 0；(xᵅ)′ = αxᵅ⁻¹（任意实数 α 时通常取 x>0）。
(1/x)′ = −1/x²，x≠0；(√x)′ = 1/(2√x)，x>0。
(eˣ)′ = eˣ；(aˣ)′ = aˣ ln a，a>0 且 a≠1。
(ln x)′ = 1/x，x>0；(ln|x|)′ = 1/x，x≠0。
(logₐx)′ = 1/(x ln a)，x>0，a>0 且 a≠1。
(sin x)′ = cos x；(cos x)′ = −sin x。
(tan x)′ = 1/cos²x；(cot x)′ = −1/sin²x。
(arcsin x)′ = 1/√(1−x²)；(arccos x)′ = −1/√(1−x²)，|x|<1。
(arctan x)′ = 1/(1+x²)。
易错：cos、cot、arccos 的导数带负号；常数的导数是 0。',1,'[{"label":"常数与幂函数","tex":"(C)''=0,\\\\qquad (x^\\\\alpha)''=\\\\alpha x^{\\\\alpha-1}","condition":"任意实数 α 时取 x>0；其他情形按定义域判断"},{"label":"倒数与根式","tex":"\\\\left(\\\\frac1x\\\\right)''=-\\\\frac1{x^2},\\\\qquad (\\\\sqrt{x})''=\\\\frac1{2\\\\sqrt{x}}","condition":"分别要求 x≠0、x>0"},{"label":"指数函数","tex":"(e^x)''=e^x,\\\\qquad (a^x)''=a^x\\\\ln a","condition":"a>0，a≠1"},{"label":"对数函数","tex":"(\\\\ln|x|)''=\\\\frac1x,\\\\qquad (\\\\log_a x)''=\\\\frac1{x\\\\ln a}","condition":"前式 x≠0；后式 x>0，a>0 且 a≠1"},{"label":"正弦与余弦","tex":"(\\\\sin x)''=\\\\cos x,\\\\qquad (\\\\cos x)''=-\\\\sin x","condition":"弧度制"},{"label":"正切与余切","tex":"(\\\\tan x)''=\\\\frac1{\\\\cos^2x},\\\\qquad (\\\\cot x)''=-\\\\frac1{\\\\sin^2x}","condition":"分母不为 0"},{"label":"反三角函数","tex":"(\\\\arcsin x)''=\\\\frac1{\\\\sqrt{1-x^2}},\\\\quad (\\\\arccos x)''=-\\\\frac1{\\\\sqrt{1-x^2}}","condition":"|x|<1"},{"label":"反正切","tex":"(\\\\arctan x)''=\\\\frac1{1+x^2}","condition":"x 为任意实数"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ab8d6098-8817-5035-8562-b56f021e1f18','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',1,'求 $y=3x^4-5x+7$ 的导数。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ab8d6098-8817-5035-8562-b56f021e1f18','$y''=12x^3-5$','各项分别求导；常数项导数为 0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c8c78f39-f831-5e45-83f8-191b817262c5','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',2,'求 $y=\\sqrt{x}+\\ln x-\\cos x$ 的导数。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c8c78f39-f831-5e45-83f8-191b817262c5','$y''=\\frac1{2\\sqrt{x}}+\\frac1x+\\sin x$，$x>0$。','根式、对数和三角函数分别套公式，注意负余弦求导变为正正弦。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('134c25ce-71dd-52bd-82d3-5bede0569a10','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',3,'求 $y=x^2\\ln x$ 在 $x=1$ 处的切线。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('134c25ce-71dd-52bd-82d3-5bede0569a10','$y=x-1$。','乘积法则给 $y''=2x\\ln x+x$。代入得切点 $(1,0)$、斜率 $1$，用点斜式。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('fd23232e-3c05-5a12-bde0-c56d8d235a3d','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',4,'求 $f(x)=\\arctan x+\\arctan(1/x)$ 在各连续区间的值。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('fd23232e-3c05-5a12-bde0-c56d8d235a3d','$x>0$ 时为 $\\pi/2$；$x<0$ 时为 $-\\pi/2$。','链式法则给 $f''=1/(1+x^2)-1/(1+x^2)=0$。分别在两个连通区间代入 $1,-1$ 定常数，不能跨过 0 合并。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('53b9ebe3-21c6-5519-83f6-d58e412e2758','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',5,'设 $f(0)=0$，$x\\ne0$ 时 $f(x)=x^2\\sin(1/x)$。求 $f''(0)$，并判断 $f''$ 在 0 处是否连续。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('53b9ebe3-21c6-5519-83f6-d58e412e2758','$f''(0)=0$；导函数在 0 处不连续。','由定义 $f''(0)=\\lim_{h\\to0}h\\sin(1/h)=0$。非零处 $f''(x)=2x\\sin(1/x)-\\cos(1/x)$；取余弦分别为 1 与 −1 的趋零数列，极限不同。可导不推出导函数连续。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d60a0736-3d6b-51a8-be34-292e6a0d8833','db3612d9-efd1-592c-af86-104ffa76daba','467c4693-6e97-5813-9a7f-df24ec6546a9',1,'例：y=3x⁴−2ln x+sin x ',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d60a0736-3d6b-51a8-be34-292e6a0d8833',' y′=12x³−2/x+cos x（x>0）。','例：y=3x⁴−2ln x+sin x ⇒ y′=12x³−2/x+cos x（x>0）。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('69561cae-9f76-5834-8e69-009abde3d2ad','afdac469-0fe4-5007-833c-51a71333967b','chain');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad','四则运算与复合函数求导','(u±v)′ = u′±v′；(Cu)′ = Cu′。
(uv)′ = u′v+uv′。
(u/v)′ = (u′v−uv′)/v²，v≠0。
链式法则：[f(g(x))]′ = f′(g(x))g′(x)。先对外层求导，再乘内层导数。
易错：(uv)′ 不是 u′v′；复合函数不能漏乘内层导数。',2,'[{"label":"四则法则","tex":"(uv)''=u'' v+uv'',\\\\qquad \\\\left(\\\\frac uv\\\\right)''=\\\\frac{u'' v-uv''}{v^2}","condition":"u、v 可导；商法则 v≠0"},{"label":"链式法则","tex":"\\\\frac{d}{dx}f(g(x))=f''(g(x))g''(x)","condition":"内外层在相应点可导"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('47dfb931-ca67-5949-8841-89eb4a5ce8b9','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',1,'求 $y=\\sin(3x)$ 的导数。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('47dfb931-ca67-5949-8841-89eb4a5ce8b9','$3\\cos(3x)$','外层正弦求导，再乘内层导数 3。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('29a551ee-48f6-5334-8ff3-2308c614eaa1','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',2,'求 $y=\\ln(1+x^2)$ 的导数。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('29a551ee-48f6-5334-8ff3-2308c614eaa1','$2x/(1+x^2)$','把 $1+x^2$ 看作整体，求对数导数再乘 $2x$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('3e84dc5b-33af-50c2-836c-bec0b044158f','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',3,'求 $y=xe^{x^2}$ 的导数。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('3e84dc5b-33af-50c2-836c-bec0b044158f','$e^{x^2}(1+2x^2)$','乘积求导后，指数项还要用链式法则。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('445fe199-feee-59ff-aa33-70249f78de9a','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',4,'求 $y=x^x$ 的导数（$x>0$）。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('445fe199-feee-59ff-aa33-70249f78de9a','$x^x(1+\\ln x)$','两边取对数：$\\ln y=x\\ln x$。求导得到 $y''/y=1+\\ln x$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('990e86e7-a53e-5e0a-81f1-51f418c7e3ef','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',5,'设 $g(x)=\\int_0^{x^2}\\ln(1+t^2)\\,dt$，求 $g''''(x)$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('990e86e7-a53e-5e0a-81f1-51f418c7e3ef','$2\\ln(1+x^4)+\\frac{8x^4}{1+x^4}$','变上限积分求导得 $g''=2x\\ln(1+x^4)$。再用乘积和复合求导：$g''''=2\\ln(1+x^4)+2x\\cdot4x^3/(1+x^4)$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9ac7a74b-9713-59db-94ef-6978755c68b8','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',2,'例1：y=sin(x²) ',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9ac7a74b-9713-59db-94ef-6978755c68b8',' y′=2x cos(x²)。','例1：y=sin(x²) ⇒ y′=2x cos(x²)。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d3eac23c-4249-5125-8e67-ed4e982ea1be','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',2,'例2：y=ln(1+x²) ',1001);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d3eac23c-4249-5125-8e67-ed4e982ea1be',' y′=2x/(1+x²)。','例2：y=ln(1+x²) ⇒ y′=2x/(1+x²)。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('03a12592-cc67-5c8c-8390-219b05b4a539','db3612d9-efd1-592c-af86-104ffa76daba','69561cae-9f76-5834-8e69-009abde3d2ad',2,'例3：y=x eˣ ',1002);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('03a12592-cc67-5c8c-8390-219b05b4a539',' y′=(1+x)eˣ。','例3：y=x eˣ ⇒ y′=(1+x)eˣ。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4','afdac469-0fe4-5007-833c-51a71333967b','implicit');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4','隐函数、参数方程与高阶导数','隐函数 F(x,y)=0：若 F 可微且 Fᵧ≠0，则 y′=−Fₓ/Fᵧ。
参数方程 x=x(t)，y=y(t)：dy/dx=y′(t)/x′(t)，要求 x′(t)≠0。
二阶导数：d²y/dx² = [d/dt(dy/dx)] / x′(t)。
高阶导数：y⁽ⁿ⁾ 表示连续求导 n 次。例 y=e²ˣ ⇒ y⁽ⁿ⁾=2ⁿe²ˣ。
微分：dy=f′(x)dx；小增量下 Δy≈f′(x)Δx。
易错：参数方程二阶导数不是 y″(t)/x″(t)。',3,'[{"label":"隐函数求导","tex":"y''=-\\\\frac{F_x}{F_y}","condition":"F 连续可微，F_y≠0"},{"label":"参数方程","tex":"\\\\frac{dy}{dx}=\\\\frac{y''(t)}{x''(t)},\\\\qquad \\\\frac{d^2y}{dx^2}=\\\\frac{\\\\frac{d}{dt}(dy/dx)}{x''(t)}","condition":"x′(t)≠0"},{"label":"微分","tex":"dy=f''(x)\\\\,dx","condition":"f 在该点可导"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('93f0107a-0540-5dea-b333-1e6dc12aa09d','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',1,'$x^2+y^2=4$，求 $y''$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('93f0107a-0540-5dea-b333-1e6dc12aa09d','$-x/y$，$y\\ne0$。','两边对 x 求导，y 是 x 的函数，故 $2x+2yy''=0$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('868c8f23-d6fe-5224-83d6-4a71d932d00d','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',2,'$x=t^2,y=t^3$，求 $dy/dx$（$t\\ne0$）。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('868c8f23-d6fe-5224-83d6-4a71d932d00d','$3t/2$','分子分母分别对参数求导，取 $3t^2/(2t)$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('453146e5-7d2d-5095-9133-f629df1a53ba','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',3,'$x=e^t,y=te^t$，求 $d^2y/dx^2$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('453146e5-7d2d-5095-9133-f629df1a53ba','$e^{-t}$','先得 $dy/dx=t+1$；再对 t 求导并除以 $dx/dt=e^t$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d2dd0b97-0b9c-5215-bcb3-8fadd79bf6f2','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',4,'曲线 $x^2+xy+y^2=3$ 在 $(1,1)$ 附近定义 $y(x)$，求 $y''''(1)$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d2dd0b97-0b9c-5215-bcb3-8fadd79bf6f2','$-2/3$','一次求导：$2x+y+(x+2y)y''=0$，得 $y''(1)=-1$。再求导：$2+2y''+2(y'')^2+(x+2y)y''''=0$，代入得结果。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('98f39ee6-52ae-5e92-a839-b4f13c67185a','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',5,'参数曲线 $x=t-\\sin t,y=1-\\cos t$，$0<t<2\\pi$。求二阶导数并说明凹凸性。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('98f39ee6-52ae-5e92-a839-b4f13c67185a','$\\frac{d^2y}{dx^2}=-\\frac1{4\\sin^4(t/2)}<0$；曲线在该区间向下凹。','$dy/dx=\\sin t/(1-\\cos t)=\\cot(t/2)$。对 t 求导得 $-\\tfrac12\\csc^2(t/2)$，再除以 $1-\\cos t=2\\sin^2(t/2)$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('63e40d8a-32a8-5759-acef-dafed1382436','db3612d9-efd1-592c-af86-104ffa76daba','f78b7c6a-17d4-52b4-a4e2-0bc6f048dda4',3,'例：x²+y²=1 ',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('63e40d8a-32a8-5759-acef-dafed1382436',' 2x+2yy′=0 ⇒ y′=−x/y（y≠0）。','例：x²+y²=1 ⇒ 2x+2yy′=0 ⇒ y′=−x/y（y≠0）。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('ab2c8ae4-c33a-5387-b437-0f1c902575c5','afdac469-0fe4-5007-833c-51a71333967b','applications');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5','洛必达法则、单调性与极值','洛必达法则：0/0 或 ∞/∞ 型，在邻域可导、分母导数非零等条件满足且 f′/g′ 的极限存在（或为无穷）时，可用导数之比求原极限。每次使用前重新检查未定式。
f′>0 ⇒ 严格递增；f′<0 ⇒ 严格递减。
驻点满足 f′(x)=0，但不一定是极值点，如 x³ 在 0 处。
若 f′(a)=0 且 f″(a)>0，则 a 是极小值点；f″(a)<0 为极大值点；f″(a)=0 时需另判。
闭区间最值：比较区间内驻点、不可导点与两端点函数值。
易错：不能把“求极值”只写成解 f′=0，必须判定。',3,'[{"label":"洛必达法则","tex":"\\\\lim\\\\frac{f(x)}{g(x)}=\\\\lim\\\\frac{f''(x)}{g''(x)}","condition":"0/0 或 ∞/∞ 型；邻域可导、g′≠0，导数比极限存在等条件成立"},{"label":"极值判别","tex":"f''(x_0)=0,\\\\quad f''''(x_0)>0\\\\Rightarrow\\\\text{局部极小}","condition":"f 在邻域二阶连续可导；二阶导数<0 则极大"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('23ce5797-5837-527c-8063-77c53e98835a','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',1,'判断 $f(x)=x^2$ 在 $(0,\\infty)$ 的单调性。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('23ce5797-5837-527c-8063-77c53e98835a','严格递增。','$f''=2x>0$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d9fc7f7a-d759-5a4a-9ecf-6ac6f3c9f47d','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',2,'求 $f(x)=x^3-3x$ 的极值。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d9fc7f7a-d759-5a4a-9ecf-6ac6f3c9f47d','在 −1 处极大值 2；在 1 处极小值 −2。','$f''=3(x^2-1)$，检查 −1、1 左右导数符号。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e8acf600-349d-5352-b328-22827971fd4b','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',3,'求 $f(x)=x^3-3x$ 在 $[-2,2]$ 上的最值。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e8acf600-349d-5352-b328-22827971fd4b','最大值 2，最小值 −2。','比较端点和驻点：$f(-2)=-2,f(-1)=2,f(1)=-2,f(2)=2$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('19092e7d-0f27-5d48-9627-1562dc7bb7e5','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',4,'证明 $x>0$ 时 $\\ln(1+x)<x$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('19092e7d-0f27-5d48-9627-1562dc7bb7e5','不等式成立。','构造 $h=x-\\ln(1+x)$，有 $h(0)=0,h''=x/(1+x)>0$，故 $h(x)>0$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('93a776d9-3e7c-5738-8708-aaec4ab776dd','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',5,'设 $f\\in C^2[0,1]$，$f(0)=f(1)=0$，$f(1/2)=1$。证明存在 $\\xi\\in(0,1)$ 使 $f''''(\\xi)=-8$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('93a776d9-3e7c-5738-8708-aaec4ab776dd','存在所需的 ξ。','构造 $g(x)=f(x)-4x(1-x)$。g 在 0、1/2、1 三点为 0。两次罗尔定理得某点 $g''''(\\xi)=0$；而 $g''''=f''''+8$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('4f594d89-9c44-53ef-a0f3-2e50a33ee6f8','db3612d9-efd1-592c-af86-104ffa76daba','ab2c8ae4-c33a-5387-b437-0f1c902575c5',3,'洛必达法则、单调性与极值 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('4f594d89-9c44-53ef-a0f3-2e50a33ee6f8','例：lim[x→0](eˣ−1−x)/x² = 1/2。','例：lim[x→0](eˣ−1−x)/x² = 1/2。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('f8144dce-a0ef-546a-b129-08eee8d8ce86','afdac469-0fe4-5007-833c-51a71333967b','integrals');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86','基本不定积分公式','∫xᵅ dx = xᵅ⁺¹/(α+1)+C，α≠−1（在适用的实数定义区间上）。
∫1/x dx = ln|x|+C，x≠0。
∫eˣ dx=eˣ+C；∫aˣ dx=aˣ/ln a+C，a>0 且 a≠1。
∫cos x dx=sin x+C；∫sin x dx=−cos x+C。
∫1/cos²x dx=tan x+C；∫1/sin²x dx=−cot x+C。
∫1/(1+x²) dx=arctan x+C。
∫1/√(1−x²) dx=arcsin x+C（|x|<1）。
易错：不定积分必须加 C；x⁻¹ 不能套幂函数积分公式。',2,'[{"label":"幂函数积分","tex":"\\\\int x^\\\\alpha\\\\,dx=\\\\frac{x^{\\\\alpha+1}}{\\\\alpha+1}+C","condition":"α≠−1；在定义域适用区间内"},{"label":"对数与指数","tex":"\\\\int\\\\frac{dx}{x}=\\\\ln|x|+C,\\\\qquad\\\\int e^x\\\\,dx=e^x+C","condition":"x≠0"},{"label":"三角函数","tex":"\\\\int\\\\sin x\\\\,dx=-\\\\cos x+C,\\\\quad\\\\int\\\\cos x\\\\,dx=\\\\sin x+C","condition":"弧度制"},{"label":"反三角型","tex":"\\\\int\\\\frac{dx}{1+x^2}=\\\\arctan x+C,\\\\quad\\\\int\\\\frac{dx}{\\\\sqrt{1-x^2}}=\\\\arcsin x+C","condition":"后式 |x|<1"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('4499df11-fad8-58c9-ac64-15b6401492be','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',1,'求 $\\int 3x^2\\,dx$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('4499df11-fad8-58c9-ac64-15b6401492be','$x^3+C$','幂次加 1 后除以新幂次。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('296bf2a5-8a71-5e34-b067-2718022b21c3','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',2,'求 $\\int(2/x+\\cos x)\\,dx$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('296bf2a5-8a71-5e34-b067-2718022b21c3','$2\\ln|x|+\\sin x+C$','按线性性质分别积分，x≠0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a2643a43-2c68-5c23-aedd-773997a66b7a','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',3,'求 $\\int\\frac{x^2}{1+x^2}\\,dx$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a2643a43-2c68-5c23-aedd-773997a66b7a','$x-\\arctan x+C$','先分解为 $1-1/(1+x^2)$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('2b218b08-67ac-59e5-8a3e-65d92e829ebe','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',4,'求 $\\int\\frac{dx}{x(x+1)}$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('2b218b08-67ac-59e5-8a3e-65d92e829ebe','$\\ln|x|-\\ln|x+1|+C$','部分分式为 $1/x-1/(x+1)$，在避开 0、−1 的区间积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b862b808-7d75-557a-86c5-7a0c876f7d0c','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',5,'求 $\\int\\frac{dx}{(1+x^2)^2}$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b862b808-7d75-557a-86c5-7a0c876f7d0c','$\\frac{x}{2(1+x^2)}+\\frac12\\arctan x+C$','令 $x=\\tan t$，积分化为 $\\int\\cos^2t\\,dt=t/2+\\sin(2t)/4+C$。用 $t=\\arctan x$ 和 $\\sin2t=2x/(1+x^2)$ 回代。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('bf371310-1fa4-5c6c-8649-3100a5b6c117','db3612d9-efd1-592c-af86-104ffa76daba','f8144dce-a0ef-546a-b129-08eee8d8ce86',2,'基本不定积分公式 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('bf371310-1fa4-5c6c-8649-3100a5b6c117','例：∫(3x²−2/x)dx=x³−2ln|x|+C。','例：∫(3x²−2/x)dx=x³−2ln|x|+C。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('33f616e8-641e-570d-a9a0-692825e86261','afdac469-0fe4-5007-833c-51a71333967b','integration-methods');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261','换元与分部积分','换元：∫f(g(x))g′(x)dx = F(g(x))+C，其中 F′=f。
分部积分：∫u dv = uv−∫v du。常把对数、多项式选作 u，但应以化简后积分更容易为准。
定积分换元：变量替换后上下限也要一起替换；若换回原变量，再用原上下限。
检验：将不定积分结果求导，应回到被积函数。',3,'[{"label":"换元积分","tex":"\\\\int f(g(x))g''(x)\\\\,dx=F(g(x))+C","condition":"F′=f；相应函数可导"},{"label":"分部积分","tex":"\\\\int u\\\\,dv=uv-\\\\int v\\\\,du","condition":"选 u 易于求导，dv 易于积分"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('446f4130-c0de-5666-ad4c-b51931f8d67e','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',1,'求 $\\int 2x e^{x^2}\\,dx$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('446f4130-c0de-5666-ad4c-b51931f8d67e','$e^{x^2}+C$','令 $u=x^2$，则 $du=2x\\,dx$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('1f1d6546-db22-5d54-b72b-c5c303b5c17e','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',2,'求 $\\int x\\cos x\\,dx$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('1f1d6546-db22-5d54-b72b-c5c303b5c17e','$x\\sin x+\\cos x+C$','取 $u=x,dv=\\cos x\\,dx$，做一次分部积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9ed13b45-2c3c-5757-b811-e0f81f197c56','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',3,'求 $\\int\\ln x\\,dx$（$x>0$）。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9ed13b45-2c3c-5757-b811-e0f81f197c56','$x\\ln x-x+C$','把被积式视作 $\\ln x\\cdot1$，取 u 为对数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('be8a7b2d-3fd6-5373-9e16-ec17f16bb11d','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',4,'求 $\\int e^x\\cos x\\,dx$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('be8a7b2d-3fd6-5373-9e16-ec17f16bb11d','$\\frac{e^x}{2}(\\sin x+\\cos x)+C$','连续分部两次，将出现的原积分移到左边，合并为两倍原积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b82350a1-cab7-598f-95e2-c3de1ea464bb','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',5,'求 $I=\\int_0^1 x^2\\ln(1+x)\\,dx$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b82350a1-cab7-598f-95e2-c3de1ea464bb','$\\frac23\\ln2-\\frac5{18}$','取 $u=\\ln(1+x),dv=x^2dx$，得到 $I=\\ln2/3-\\tfrac13\\int_0^1 x^3/(1+x)dx$。多项式除法 $x^3/(1+x)=x^2-x+1-1/(1+x)$，积分为 $5/6-\\ln2$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7ec3fd7a-6c5e-5e12-a7f1-bf84dacac5bf','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',3,'换元与分部积分 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7ec3fd7a-6c5e-5e12-a7f1-bf84dacac5bf','例：∫2x cos(x²)dx=sin(x²)+C，令 u=x²。','例：∫2x cos(x²)dx=sin(x²)+C，令 u=x²。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('da01b9c1-862a-52e5-a9d3-f07b86506277','db3612d9-efd1-592c-af86-104ffa76daba','33f616e8-641e-570d-a9a0-692825e86261',3,'换元与分部积分 · 教学示例',1001);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('da01b9c1-862a-52e5-a9d3-f07b86506277','例：∫x eˣdx = x eˣ−∫eˣdx=(x−1)eˣ+C。','例：∫x eˣdx = x eˣ−∫eˣdx=(x−1)eˣ+C。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('6d15607f-aa72-57c9-b626-284050109d37','afdac469-0fe4-5007-833c-51a71333967b','definite');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37','定积分、反常积分与面积','微积分基本定理：若 f 连续且 F′=f，则 ∫[a,b]f(x)dx=F(b)−F(a)。
变上限：d/dx ∫[a,x]f(t)dt=f(x)；上限为 g(x) 时再乘 g′(x)。
几何面积：S=∫[a,b]|f(x)|dx；两曲线间面积按上减下分段积分。
旋转体：绕 x 轴的圆盘法 V=π∫[a,b]f(x)²dx（相应区域满足圆盘条件）。
反常积分：先改写为极限，再判断是否收敛。∫[1,∞]x⁻ᵖdx 在 p>1 时收敛；∫[0,1]x⁻ᵖdx 在 p<1 时收敛。
易错：定积分允许正负抵消，面积不允许。',3,'[{"label":"牛顿—莱布尼茨公式","tex":"\\\\int_a^b f(x)\\\\,dx=F(b)-F(a)","condition":"f 连续且 F′=f"},{"label":"变上限积分","tex":"\\\\frac d{dx}\\\\int_a^{g(x)}f(t)\\\\,dt=f(g(x))g''(x)","condition":"f 连续，g 可导"},{"label":"两曲线间面积","tex":"S=\\\\int_a^b|f(x)-g(x)|\\\\,dx","condition":"交点处分段，面积不带符号"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('88d0fc5a-1b14-50f3-a626-dec0c007f832','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',1,'计算 $\\int_0^1 2x\\,dx$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('88d0fc5a-1b14-50f3-a626-dec0c007f832','$1$','原函数为 $x^2$，上限值减下限值。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('76cf455d-a23c-5e4a-bd2e-6b1c5b21319c','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',2,'求曲线 $y=x$ 与 $y=x^2$ 围成的面积。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('76cf455d-a23c-5e4a-bd2e-6b1c5b21319c','$1/6$','交点 x=0、1，区间内 x 在上，积分 $\\int_0^1(x-x^2)dx$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('316f29a4-2f82-5864-9d89-1a0d2e51c1d9','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',3,'判断 $\\int_1^\\infty x^{-p}dx$ 的敛散性。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('316f29a4-2f82-5864-9d89-1a0d2e51c1d9','$p>1$ 收敛，值 $1/(p-1)$；否则发散。','先在 [1,R] 积分再令 R→∞；p=1 单独用对数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('59d5d568-fa27-5edf-8ae1-55d589598715','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',4,'求 $\\frac d{dx}\\int_x^{x^2}e^{t^2}dt$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('59d5d568-fa27-5edf-8ae1-55d589598715','$2xe^{x^4}-e^{x^2}$','将上下限变化分别处理，上限项减下限项。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c188c076-1c25-5598-951c-30b0552e0a9f','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',5,'计算反常积分 $I=\\int_0^1 x\\ln\\frac{1+x}{1-x}\\,dx$，并说明端点处为何仍可收敛。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c188c076-1c25-5598-951c-30b0552e0a9f','$I=1$。','先在 $[0,b]$（$b<1$）分部积分，取 $u=\\ln\\frac{1+x}{1-x},\\ dv=x\\,dx$，得 $I(b)=b+\\frac{b^2-1}{2}\\ln\\frac{1+b}{1-b}$。当 $b\\to1^-$，第二项趋于 0，因为 $t\\ln t\\to0$（$t\\to0^+$）。所以积分收敛且为 1；不能直接把发散边界项与发散积分分别代入。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('6b17c72a-d1ac-50b2-855f-893bc8de7545','db3612d9-efd1-592c-af86-104ffa76daba','6d15607f-aa72-57c9-b626-284050109d37',3,'定积分、反常积分与面积 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('6b17c72a-d1ac-50b2-855f-893bc8de7545','例：∫[0,1]x²dx=1/3。','例：∫[0,1]x²dx=1/3。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('4cd295c7-4488-5851-be45-d83507139d54','afdac469-0fe4-5007-833c-51a71333967b','vectors');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54','空间向量、直线与平面','a·b=a₁b₁+a₂b₂+a₃b₃=|a||b|cosθ；非零向量垂直当且仅当点积为 0。
|a×b|=|a||b|sinθ，叉积方向垂直于两向量。
过点 P₀、法向量 n=(A,B,C) 的平面：A(x−x₀)+B(y−y₀)+C(z−z₀)=0。
直线参数式：(x,y,z)=(x₀,y₀,z₀)+t(a,b,c)，方向向量非零。
点到平面 Ax+By+Cz+D=0 距离：|Ax₀+By₀+Cz₀+D|/√(A²+B²+C²)。
易错：直线与平面的夹角用方向向量与法向量夹角的余角。',2,'[{"label":"数量积","tex":"\\\\mathbf a\\\\cdot\\\\mathbf b=|\\\\mathbf a||\\\\mathbf b|\\\\cos\\\\theta=\\\\sum_{i=1}^3a_ib_i","condition":"非零向量夹角 θ"},{"label":"平面方程","tex":"A(x-x_0)+B(y-y_0)+C(z-z_0)=0","condition":"法向量 (A,B,C)≠0"},{"label":"点到平面距离","tex":"d=\\\\frac{|Ax_0+By_0+Cz_0+D|}{\\\\sqrt{A^2+B^2+C^2}}","condition":"平面 Ax+By+Cz+D=0"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('64a27f2d-fc56-5e51-a63f-abb3983b24c2','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',1,'求向量 $(1,2,2)$ 的模。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('64a27f2d-fc56-5e51-a63f-abb3983b24c2','$3$','平方和开根号。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ca7c2225-bf03-539f-ac02-915641eaee9f','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',2,'求 $(1,1,0)$ 与 $(1,0,1)$ 的夹角。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ca7c2225-bf03-539f-ac02-915641eaee9f','$\\pi/3$','点积 1，两模均为根号 2，夹角余弦为 1/2。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a655675a-d762-5417-8d21-f07ac75cf6c8','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',3,'过 $(1,0,2)$ 且法向量为 $(2,-1,1)$ 的平面方程是什么？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a655675a-d762-5417-8d21-f07ac75cf6c8','$2x-y+z-4=0$','将点和法向量代入点法式。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c32e32b1-a8bf-5b11-9d20-f5205b8510b9','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',4,'求点 $(1,1,1)$ 到平面 $x+2y+2z=0$ 的距离。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c32e32b1-a8bf-5b11-9d20-f5205b8510b9','$5/3$','代入距离公式，分母为 3。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e9e6a6f0-b157-5708-9e8c-e7a870bac68c','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',5,'求直线 $L_1:(x,y,z)=(t,0,0)$ 与 $L_2:(x,y,z)=(0,1+s,1-s)$ 的公垂线长度和两个垂足。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e9e6a6f0-b157-5708-9e8c-e7a870bac68c','距离 $\\sqrt2$；垂足分别为 $(0,0,0)$、$(0,1,1)$。','两方向向量为 $(1,0,0)$、$(0,1,-1)$，叉积为 $(0,1,1)$。连接向量为 $(-t,1+s,1-s)$，分别与两方向点积为零得 t=s=0。长度为根号 2；也可用混合积除以叉积模验证。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a6c3bf10-aa99-5fc5-9bd2-208dcae8b175','db3612d9-efd1-592c-af86-104ffa76daba','4cd295c7-4488-5851-be45-d83507139d54',2,'空间向量、直线与平面 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a6c3bf10-aa99-5fc5-9bd2-208dcae8b175','例：点 (1,2,3) 到 z=0 的距离是 3。','例：点 (1,2,3) 到 z=0 的距离是 3。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('5e6467ed-8527-5f32-9216-4f68dd2bc3f3','afdac469-0fe4-5007-833c-51a71333967b','partial');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3','偏导数、全微分与链式法则','对 x 求偏导时，将其他独立变量视为常数。
z=f(x,y) 可微时：dz=fₓdx+fᵧdy。偏导存在本身不保证可微；偏导在邻域连续是常用充分条件。
若 z=f(u,v)，u=u(x,y)，v=v(x,y)，则 zₓ=fᵤuₓ+fᵥvₓ。
二阶混合偏导在邻域连续时 fₓᵧ=fᵧₓ。
隐函数 F(x,y,z)=0 且 F_z≠0 时：zₓ=−Fₓ/F_z，zᵧ=−Fᵧ/F_z。',3,'[{"label":"全微分","tex":"dz=f_x\\\\,dx+f_y\\\\,dy","condition":"f 可微；偏导存在本身不充分"},{"label":"多元链式法则","tex":"\\\\frac{dz}{dt}=f_x\\\\frac{dx}{dt}+f_y\\\\frac{dy}{dt}","condition":"z=f(x(t),y(t))，相应函数可微"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('89b187c9-b89e-5677-884c-8e0827128560','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',1,'$f=x^2y$，求 $f_x$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('89b187c9-b89e-5677-884c-8e0827128560','$2xy$','把 y 视为常数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('4fb29497-60e5-51ef-8de1-eb8c5b85a471','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',2,'$f=e^{xy}$，求 $f_y$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('4fb29497-60e5-51ef-8de1-eb8c5b85a471','$xe^{xy}$','对指数内的 xy 求 y 导数，得到 x。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e6c3aaa7-ee36-5df5-bcec-c5bad1a7ae82','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',3,'$z=x^2+y^2,x=t,y=t^2$，求 $dz/dt$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e6c3aaa7-ee36-5df5-bcec-c5bad1a7ae82','$2t+4t^3$','链式法则给 $2x+2y(2t)$，代入参数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('28c48b17-edc6-5fd5-8dfe-dd531d214579','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',4,'$z=\\ln(x^2+y^2)$，求 $z_{xy}$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('28c48b17-edc6-5fd5-8dfe-dd531d214579','$-4xy/(x^2+y^2)^2$，$(x,y)\\ne(0,0)$。','先求 $z_x=2x/(x^2+y^2)$，再对 y 求导。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7e021c5a-959c-592c-96fa-06199c23f146','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',5,'设 $f(0,0)=0$，其他点 $f(x,y)=xy/\\sqrt{x^2+y^2}$。判断原点连续性、两个偏导及可微性。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7e021c5a-959c-592c-96fa-06199c23f146','连续；两偏导均为 0；不可微。','由 $|xy|\\le(x^2+y^2)/2$ 得 $|f|\\le\\sqrt{x^2+y^2}/2$，故连续。两坐标轴上函数恒为 0，偏导为 0。沿 x=y=t，误差除以距离等于 1/2，不能趋 0，故不可微。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ae45df9a-50e8-5f62-b849-c25af63f2f17','db3612d9-efd1-592c-af86-104ffa76daba','5e6467ed-8527-5f32-9216-4f68dd2bc3f3',3,'偏导数、全微分与链式法则 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ae45df9a-50e8-5f62-b849-c25af63f2f17','例：f=x²y+sin y，则 fₓ=2xy，fᵧ=x²+cos y，df=2xy dx+(x²+cos y)dy。','例：f=x²y+sin y，则 fₓ=2xy，fᵧ=x²+cos y，df=2xy dx+(x²+cos y)dy。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('7907f33a-9983-5eed-b6e9-08585c546907','afdac469-0fe4-5007-833c-51a71333967b','multivariate-applications');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907','多元极值、梯度与切平面','梯度 ∇f=(fₓ,fᵧ,f_z)。沿单位向量 e 的方向导数为 ∇f·e（f 可微）。
曲面 F(x,y,z)=0 在 P 处 ∇F(P)≠0 时，切平面：∇F(P)·(X−P)=0。
二元无约束极值：先解 fₓ=fᵧ=0，再令 D=fₓₓfᵧᵧ−fₓᵧ²。D>0 且 fₓₓ>0 为极小；D>0 且 fₓₓ<0 为极大；D<0 为鞍点；D=0 需另判。
约束 g=0：满足正则条件时用 ∇f=λ∇g 联立约束，最后比较候选点与边界。',4,'[{"label":"二元极值判别","tex":"D=f_{xx}f_{yy}-f_{xy}^2","condition":"驻点处 D>0 且 f_xx>0 为极小，f_xx<0 为极大；D<0 为鞍点；D=0 不确定"},{"label":"拉格朗日乘子","tex":"\\\\nabla f=\\\\lambda\\\\nabla g,\\\\qquad g=0","condition":"约束梯度非零的必要条件，还需比较候选值"},{"label":"切平面","tex":"z-z_0=f_x(x_0,y_0)(x-x_0)+f_y(x_0,y_0)(y-y_0)","condition":"曲面 z=f(x,y) 在该点可微"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('cb3aeea2-d6d5-5a83-b529-39262de40068','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',1,'求 $f=x^2+y^2$ 的驻点。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('cb3aeea2-d6d5-5a83-b529-39262de40068','(0,0)。','令两个偏导 2x、2y 同时为 0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b9b05474-d9fd-5ac5-9f4e-702c6ecbe431','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',2,'判定 $f=x^2-y^2$ 在原点的类型。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b9b05474-d9fd-5ac5-9f4e-702c6ecbe431','鞍点。','沿 x 轴取正，沿 y 轴取负；或 Hessian 行列式为 −4。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c2dc62af-c9e9-5cba-bd9e-eaa856a26b1e','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',3,'求 $z=x^2+y^2$ 在 $(1,1,2)$ 处的切平面。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c2dc62af-c9e9-5cba-bd9e-eaa856a26b1e','$z=2x+2y-2$','偏导值均为 2，代入切平面公式。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('abc221f6-5391-531c-8bd3-a291f15387f8','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',4,'在 $x^2+y^2=1$ 上求 $x+y$ 的最值。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('abc221f6-5391-531c-8bd3-a291f15387f8','最大值 $\\sqrt2$，最小值 $-\\sqrt2$。','由柯西不等式或拉格朗日乘子得到 x=y，分别取同号的 $1/\\sqrt2$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('093be15f-1a8c-5b0c-98e4-b9ca4df59e29','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',5,'在圆盘 $x^2+y^2\\le1$ 上求 $f=x^2+2y^2-2x$ 的最大、最小值。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('093be15f-1a8c-5b0c-98e4-b9ca4df59e29','最小值 −1，在 (1,0)；最大值 3，在 (−1,0)。','内部驻点候选满足 x=1,y=0，落在边界。边界代入 $y^2=1-x^2$ 得 $f=2-x^2-2x=3-(x+1)^2$，x∈[−1,1]，最小 −1，最大 3。对内部亦可写 $(x-1)^2+2y^2-1\\ge-1$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('82e0075d-0e71-5e21-b823-14c59107c047','db3612d9-efd1-592c-af86-104ffa76daba','7907f33a-9983-5eed-b6e9-08585c546907',4,'多元极值、梯度与切平面 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('82e0075d-0e71-5e21-b823-14c59107c047','例：f=x²+y² 在 (0,0) 取最小值 0。','例：f=x²+y² 在 (0,0) 取最小值 0。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('1531a834-f2a1-5045-8da9-57f260860a0b','afdac469-0fe4-5007-833c-51a71333967b','multiple-integrals');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b','二重积分、三重积分与换元','二重积分 ∫∫D f(x,y)dA：先画区域，再决定积分次序。若 a≤x≤b，φ₁(x)≤y≤φ₂(x)，则先对 y 积分。
极坐标：x=r cosθ，y=r sinθ，dA=r dr dθ，不能漏 r。
三重积分求体积：V=∫∫∫Ω 1 dV。
一般换元：面积或体积元乘雅可比行列式绝对值。
对称性：区域关于 x 轴对称，且 f 对 y 为奇函数时，积分为 0（积分存在）。',4,'[{"label":"二重积分","tex":"\\\\iint_D f\\\\,dA=\\\\int_a^b\\\\!\\\\int_{\\\\varphi_1(x)}^{\\\\varphi_2(x)}f(x,y)\\\\,dy\\\\,dx","condition":"区域按 x 切片；满足可积条件"},{"label":"极坐标换元","tex":"x=r\\\\cos\\\\theta,\\\\quad y=r\\\\sin\\\\theta,\\\\quad dA=r\\\\,dr\\\\,d\\\\theta","condition":"r≥0；不可漏 Jacobian 因子 r"},{"label":"三维球坐标体积元","tex":"dV=\\\\rho^2\\\\sin\\\\varphi\\\\,d\\\\rho\\\\,d\\\\varphi\\\\,d\\\\theta","condition":"φ 为与 z 轴夹角"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e701d6fa-c97a-5438-bdbb-01e8ef42963a','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',1,'计算单位正方形上 $\\iint_D1\\,dA$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e701d6fa-c97a-5438-bdbb-01e8ef42963a','$1$','常数 1 的积分就是面积。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a8cfda22-fc7e-5e4e-83cd-cceb69eaa877','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',2,'计算 $\\int_0^1\\int_0^2 xy\\,dy\\,dx$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a8cfda22-fc7e-5e4e-83cd-cceb69eaa877','$1$','先对 y 积分得 2x，再对 x 积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('6b1f8901-70f7-5b3f-a413-d4a0bd6fc9dd','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',3,'计算单位圆盘上 $\\iint_D(x^2+y^2)\\,dA$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('6b1f8901-70f7-5b3f-a413-d4a0bd6fc9dd','$\\pi/2$','极坐标变为 $\\int_0^{2\\pi}\\int_0^1r^3drd\\theta$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('30a0c5bd-0416-5bd0-899d-605a5ffca70a','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',4,'交换 $\\int_0^1\\int_x^1 e^{y^2}\\,dy\\,dx$ 的次序并计算。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('30a0c5bd-0416-5bd0-899d-605a5ffca70a','$(e-1)/2$','区域 0≤x≤y≤1，交换成 $\\int_0^1\\int_0^y e^{y^2}dxdy=\\int_0^1ye^{y^2}dy$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b552c4ef-c596-5cf5-88a0-83b25c631061','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',5,'计算球体 $x^2+y^2+z^2\\le a^2$（$a>0$）上 $\\iiint(x^2+y^2)\\,dV$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b552c4ef-c596-5cf5-88a0-83b25c631061','$8\\pi a^5/15$','由对称性，目标为 $\\tfrac23\\iiint(x^2+y^2+z^2)dV$。球坐标得后者 $4\\pi\\int_0^a\\rho^4d\\rho=4\\pi a^5/5$，乘 2/3。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('206931fa-cbc8-5e5b-9084-249953979bfa','db3612d9-efd1-592c-af86-104ffa76daba','1531a834-f2a1-5045-8da9-57f260860a0b',4,'二重积分、三重积分与换元 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('206931fa-cbc8-5e5b-9084-249953979bfa','例：单位圆盘上 ∫∫D1 dA=∫[0,2π]∫[0,1]r dr dθ=π。','例：单位圆盘上 ∫∫D1 dA=∫[0,2π]∫[0,1]r dr dθ=π。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('018aa5b8-67a3-5997-aecd-88dbfdd1a28a','afdac469-0fe4-5007-833c-51a71333967b','line-surface');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a','曲线积分与曲面积分','第一类曲线积分：∫L f ds；参数化 r(t) 后为 ∫f(r(t))|r′(t)|dt，反向不变。
第二类：∫L Pdx+Qdy+Rdz；参数化后为 ∫[Px′+Qy′+Rz′]dt，反向变号。
格林公式：对正向（逆时针）简单闭曲线及内部区域，在 P、Q 一阶偏导连续等条件下，∮Pdx+Qdy=∫∫(Qₓ−Pᵧ)dA。
第一类曲面积分：z=z(x,y) 时 dS=√(1+zₓ²+zᵧ²)dxdy。
通量：∫∫S F·n dS，需要指定单位法向量方向。',4,'[{"label":"格林公式","tex":"\\\\oint_{\\\\partial D}P\\\\,dx+Q\\\\,dy=\\\\iint_D(Q_x-P_y)\\\\,dA","condition":"正向边界；P、Q 在包含闭区域的开集上一阶连续可微"},{"label":"高斯公式","tex":"\\\\iint_{\\\\partial V}\\\\mathbf F\\\\cdot\\\\mathbf n\\\\,dS=\\\\iiint_V\\\\nabla\\\\cdot\\\\mathbf F\\\\,dV","condition":"外法向；场在区域内满足光滑条件"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ad48858c-6a4d-5f60-b604-ef2e7d692914','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',1,'沿 x 轴从 0 到 1 求 $\\int x\\,dx$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ad48858c-6a4d-5f60-b604-ef2e7d692914','$1/2$','直接按参数 x 积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5b7b7a4f-15cb-5795-80d5-e082a0e70f79','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',2,'沿单位圆求第一类曲线积分 $\\int_L1\\,ds$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5b7b7a4f-15cb-5795-80d5-e082a0e70f79','$2\\pi$','等于曲线长度，与方向无关。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5d14d48d-5048-53c8-8c76-cd93a14af916','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',3,'沿单位圆逆时针求 $\\oint x\\,dy-y\\,dx$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5d14d48d-5048-53c8-8c76-cd93a14af916','$2\\pi$','格林公式中 $Q_x-P_y=2$，乘圆面积。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('238d9424-a1cf-5394-865d-152fa864831d','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',4,'求向量场 $(x,y,z)$ 穿过半径 a 球面向外的通量。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('238d9424-a1cf-5394-865d-152fa864831d','$4\\pi a^3$','散度为 3，乘球体积 $4\\pi a^3/3$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('f9ab04c2-73d7-52f9-96fb-eb1c6ee94bc0','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',5,'沿单位圆逆时针求 $\\oint\\frac{-y\\,dx+x\\,dy}{x^2+y^2}$，并解释为什么不能直接用零旋度断言积分为 0。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('f9ab04c2-73d7-52f9-96fb-eb1c6ee94bc0','$2\\pi$。','参数 x=cos t,y=sin t 后被积式为 dt，积分为 2π。原点是奇点，场并非在整个圆盘光滑，格林公式的条件不成立；路径无关还需考虑区域条件。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('75ce0d67-44b8-5967-8794-b6b7f7d88b3e','db3612d9-efd1-592c-af86-104ffa76daba','018aa5b8-67a3-5997-aecd-88dbfdd1a28a',4,'曲线积分与曲面积分 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('75ce0d67-44b8-5967-8794-b6b7f7d88b3e','例：单位圆上 ∮(−y dx+x dy)=2π（逆时针）。','例：单位圆上 ∮(−y dx+x dy)=2π（逆时针）。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('a7d6ce55-c8b1-5526-ab7a-d990d8840405','afdac469-0fe4-5007-833c-51a71333967b','ode');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405','常微分方程','可分离变量：y′=g(x)h(y)。在 h(y)≠0 的区间分离为 dy/h(y)=g(x)dx，并单独检查 h(y)=0 的常数解。
一阶线性：y′+P(x)y=Q(x)。积分因子 μ=e^(∫Pdx)，解为 y=μ⁻¹(∫μQdx+C)。
二阶常系数齐次：y″+ay′+by=0，解特征方程 r²+ar+b=0。
不同实根：C₁e^(r₁x)+C₂e^(r₂x)；重根 r：(C₁+C₂x)e^(rx)；复根 α±βi：e^(αx)(C₁cosβx+C₂sinβx)。
易错：求通解后仍需代入初值确定常数。',4,'[{"label":"可分离变量","tex":"\\\\frac{dy}{g(y)}=f(x)\\\\,dx","condition":"分离时另查 g(y)=0 的常数解"},{"label":"一阶线性方程","tex":"y''+p(x)y=q(x),\\\\quad y=e^{-\\\\int p\\\\,dx}\\\\left(\\\\int qe^{\\\\int p\\\\,dx}\\\\,dx+C\\\\right)","condition":"p、q 在区间连续"},{"label":"二阶常系数","tex":"y''''+ay''+by=0\\\\ \\\\Rightarrow\\\\ r^2+ar+b=0","condition":"特征根决定齐次通解"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('117eca55-2645-5db4-9459-70e1dc156fb2','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',1,'解 $y''=2x$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('117eca55-2645-5db4-9459-70e1dc156fb2','$y=x^2+C$','两边积分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('8a91c307-10bd-5168-9b97-eeea081391b0','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',2,'解 $y''=y,y(0)=2$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('8a91c307-10bd-5168-9b97-eeea081391b0','$y=2e^x$','通解 Ceˣ，初值确定 C=2。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e17595bd-dd06-5138-9b9f-d299ae0eea1e','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',3,'解 $y''+y=e^x$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e17595bd-dd06-5138-9b9f-d299ae0eea1e','$y=e^x/2+Ce^{-x}$','积分因子 eˣ：$(e^xy)''=e^{2x}$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9264ffd3-eee5-5cd1-951e-cd183ea6a6c4','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',4,'解 $y''''-3y''+2y=0$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9264ffd3-eee5-5cd1-951e-cd183ea6a6c4','$y=C_1e^x+C_2e^{2x}$','特征方程 (r−1)(r−2)=0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('958adcfe-a843-54d7-ba2f-db2aed91b3a7','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',5,'解 $y''''-2y''+y=e^x$，$y(0)=y''(0)=0$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('958adcfe-a843-54d7-ba2f-db2aed91b3a7','$y=x^2e^x/2$','令 y=eˣv，左端化为 eˣv″，所以 v″=1。积分 v=x²/2+Ax+B。初值得 B=0、A=0；体现重根共振需乘 x²。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('758f7563-0026-5109-8536-713c2ae3d1cb','db3612d9-efd1-592c-af86-104ffa76daba','a7d6ce55-c8b1-5526-ab7a-d990d8840405',4,'例：y′+y=0，y(0)=2 ',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('758f7563-0026-5109-8536-713c2ae3d1cb',' y=2e⁻ˣ。','例：y′+y=0，y(0)=2 ⇒ y=2e⁻ˣ。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a','afdac469-0fe4-5007-833c-51a71333967b','series');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a','无穷级数与幂级数','级数 ∑aₙ 收敛 ⇒ aₙ→0；反过来不成立，如调和级数 ∑1/n。
几何级数 ∑[n=0..∞]qⁿ 在 |q|<1 时和为 1/(1−q)。p 级数 ∑1/nᵖ 在 p>1 时收敛。
比值判别：lim|aₙ₊₁/aₙ|=ρ，ρ<1 绝对收敛，ρ>1 发散，ρ=1 失效。
交错级数：正项大小单调趋于 0 时，交错和收敛。绝对收敛一定收敛。
幂级数：先求收敛半径 R，再分别检查两个端点。
常用展开：eˣ=∑xⁿ/n!（全体实数）；sin x=∑(−1)ⁿx²ⁿ⁺¹/(2n+1)!；ln(1+x)=∑[n≥1](−1)ⁿ⁻¹xⁿ/n（−1<x≤1）。',4,'[{"label":"几何级数","tex":"\\\\sum_{n=0}^\\\\infty x^n=\\\\frac1{1-x}","condition":"|x|<1"},{"label":"常用幂级数","tex":"e^x=\\\\sum_{n=0}^\\\\infty\\\\frac{x^n}{n!},\\\\quad \\\\ln(1+x)=\\\\sum_{n=1}^\\\\infty\\\\frac{(-1)^{n-1}x^n}{n}","condition":"指数式全实数成立；对数式 −1<x≤1"},{"label":"比值审敛","tex":"L=\\\\lim_{n\\\\to\\\\infty}\\\\left|\\\\frac{a_{n+1}}{a_n}\\\\right|","condition":"L<1 绝对收敛，L>1 发散；L=1 不确定"}]','[{"kind":"LINK","label":"OpenStax · 微积分基础","url":"https://openstax.org/books/calculus-volume-1/pages/3-1-defining-the-derivative","fileId":null},{"kind":"LINK","label":"OpenStax · 多元微积分","url":"https://openstax.org/books/calculus-volume-3/pages/4-3-partial-derivatives","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9132e6a8-f742-5398-ae7d-ad89854ef3c0','db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a',1,'求 $\\sum_{n=0}^\\infty(1/2)^n$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9132e6a8-f742-5398-ae7d-ad89854ef3c0','$2$','几何级数首项 1、公比 1/2。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('58233941-f584-5574-bb32-ae73fa9cfd8f','db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a',2,'判断 $\\sum_{n=1}^\\infty1/n$ 是否收敛。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('58233941-f584-5574-bb32-ae73fa9cfd8f','发散。','调和级数发散，可用积分判别；通项趋零不充分。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('2763e548-f0eb-5c1a-ba24-3879028c49dd','db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a',3,'求 $\\sum_{n=1}^\\infty x^n/n$ 的收敛区间。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('2763e548-f0eb-5c1a-ba24-3879028c49dd','$[-1,1)$','比值得半径 1；x=1 调和发散，x=−1 交错收敛。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('2c52b131-5c4f-548b-996a-ff20f0106167','db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a',4,'求 $\\sum_{n=1}^\\infty nx^n$（$|x|<1$）。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('2c52b131-5c4f-548b-996a-ff20f0106167','$x/(1-x)^2$','在收敛半径内对几何级数逐项求导，再乘 x。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a45be1b7-0544-5359-a5b1-8a3dc264cab4','db3612d9-efd1-592c-af86-104ffa76daba','b7bc3f8b-29e7-5b98-873f-0a448cfa1f1a',5,'求 $S(x)=\\sum_{n=1}^\\infty\\frac{x^n}{n(n+1)}$ 的收敛区间与和函数，并求 $S(1)$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a45be1b7-0544-5359-a5b1-8a3dc264cab4','区间 $[-1,1]$；$x\\ne0$ 时 $S=1+\\frac{1-x}{x}\\ln(1-x)$（$-1\\le x<1$）；$S(0)=0,S(1)=1$。','端点由 $1/[n(n+1)]$ 可和得绝对收敛。利用 $1/[n(n+1)]=1/n-1/(n+1)$ 及对数级数合并得和式；x=0 取极限，x=1 用裂项相消或左极限得 1。');
UPDATE content_release SET state='RETIRED' WHERE id='2a075740-9067-533f-823b-f721b3a73140';
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','ccd819bb-a48b-5c75-bfbf-e26219023fef',2,'PUBLISHED',UTC_TIMESTAMP(6),'e76ee792e5876b3a68be53cb32bee157356c3f3b69c71cacaf83dc8b8000d980');
INSERT INTO chapter_revision SELECT '9fa5e36d-08a0-5370-855e-2d2cd3c3d545',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='bc3169f1-12a5-5863-9c9a-325258124830';
INSERT INTO item_revision SELECT '9fa5e36d-08a0-5370-855e-2d2cd3c3d545',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='bc3169f1-12a5-5863-9c9a-325258124830';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('63a80716-ded4-5ad5-9046-a40c029444ac','ccd819bb-a48b-5c75-bfbf-e26219023fef','logic');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','63a80716-ded4-5ad5-9046-a40c029444ac','命题逻辑与推理','p→q 等价于 ¬p∨q；逆否命题 ¬q→¬p 与原命题等价。
德摩根律：¬(p∧q)=¬p∨¬q；¬(p∨q)=¬p∧¬q。
蕴涵只有“前真后假”时为假。真值表有 n 个独立命题变元时共 2ⁿ 行。
易错：逆命题 q→p 一般不等价于 p→q。',2,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('cf76af3d-e337-58ce-8aa7-4b099b724c11','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','63a80716-ded4-5ad5-9046-a40c029444ac',1,'p 真、q 假时，p→q 为真吗？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('cf76af3d-e337-58ce-8aa7-4b099b724c11','假。','蕴涵仅在前真后假时为假。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b6bea79d-5973-50e9-a3b5-90f9c2aa0799','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','63a80716-ded4-5ad5-9046-a40c029444ac',3,'化简 ¬(p→q)。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b6bea79d-5973-50e9-a3b5-90f9c2aa0799','p∧¬q。','先把蕴涵换为 ¬p∨q，再用德摩根律。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d151b129-beb4-5b96-bcb2-2fee5e522307','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','63a80716-ded4-5ad5-9046-a40c029444ac',5,'证明 ((p→q)∧(q→r))→(p→r) 为重言式。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d151b129-beb4-5b96-bcb2-2fee5e522307','是重言式。','若前件假，整体真；若前件真：p 假时 p→r 真，p 真时由两次假言推理得 q 真、r 真，因此后件也真。覆盖所有情况。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('2b74e615-9746-5d68-801a-2bcf4ead8b0e','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','63a80716-ded4-5ad5-9046-a40c029444ac',2,'命题逻辑与推理 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('2b74e615-9746-5d68-801a-2bcf4ead8b0e','例：p→q 与 p 同时为真，可推出 q（假言推理）。','例：p→q 与 p 同时为真，可推出 q（假言推理）。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('4773cfdf-5483-5dd2-bebc-d8e1c7ef602f','ccd819bb-a48b-5c75-bfbf-e26219023fef','predicates');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','4773cfdf-5483-5dd2-bebc-d8e1c7ef602f','谓词、量词与否定','∀x P(x)：论域内所有 x 都满足 P；∃x P(x)：至少存在一个。
¬∀xP(x) ⇔ ∃x¬P(x)；¬∃xP(x) ⇔ ∀x¬P(x)。
量词顺序通常不可互换：∀x∃y 与 ∃y∀x 含义不同。
证明全称命题要覆盖任意对象；反驳全称命题给一个反例即可。',2,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('cc357f8d-84e6-5263-87fd-effc01df2be4','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','4773cfdf-5483-5dd2-bebc-d8e1c7ef602f',1,'否定“所有学生都及格”。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('cc357f8d-84e6-5263-87fd-effc01df2be4','至少有一个学生不及格。','全称量词否定变存在，谓词同时否定。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('35122157-edd2-5d8a-a5db-eedcdd10abdf','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','4773cfdf-5483-5dd2-bebc-d8e1c7ef602f',3,'在实数域，∀x∃y(x+y=0) 是否成立？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('35122157-edd2-5d8a-a5db-eedcdd10abdf','成立。','对任意 x 选 y=−x 即可，y 可以依赖 x。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('fee817f7-3081-54f9-8088-e00d4d0abf9f','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','4773cfdf-5483-5dd2-bebc-d8e1c7ef602f',5,'比较实数域的 ∀x∃y(x<y) 与 ∃y∀x(x<y)。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('fee817f7-3081-54f9-8088-e00d4d0abf9f','前者真，后者假。','前者可选 y=x+1。后者若有固定 y，选 x=y 则 x<y 为假，构成反证。注意量词依赖关系。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('bc244177-3b89-538b-a0c9-94d8edfc2078','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','4773cfdf-5483-5dd2-bebc-d8e1c7ef602f',2,'谓词、量词与否定 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('bc244177-3b89-538b-a0c9-94d8edfc2078','例：“每个学生都通过”否定为“至少一个学生未通过”。','例：“每个学生都通过”否定为“至少一个学生未通过”。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('8411d829-0a5e-5862-98db-78a776c84552','ccd819bb-a48b-5c75-bfbf-e26219023fef','sets');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','8411d829-0a5e-5862-98db-78a776c84552','集合与计数','A−B=A∩Bᶜ。幂集 P(A) 为 A 的全部子集；|A|=n 时 |P(A)|=2ⁿ。
有限集合：|A∪B|=|A|+|B|−|A∩B|。
排列 P(n,k)=n!/(n−k)!；组合 C(n,k)=n!/[k!(n−k)!]。
鸽巢原理：n+1 个对象放入 n 个盒子，至少一个盒子有不少于 2 个对象。',2,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('00e27cde-90e8-5460-8675-21815f5b5d6e','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','8411d829-0a5e-5862-98db-78a776c84552',1,'三元素集合的子集数是多少？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('00e27cde-90e8-5460-8675-21815f5b5d6e','8。','每个元素有选与不选两种选择，2³=8。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('992ecdbc-7491-5088-b399-b0c9df519ba9','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','8411d829-0a5e-5862-98db-78a776c84552',3,'A、B 分别有 20、15 个元素，交集 6 个，并集多少？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('992ecdbc-7491-5088-b399-b0c9df519ba9','29。','20+15−6，避免交集重复计算。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7e15192d-6a52-5351-b3ae-a635fcab618d','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','8411d829-0a5e-5862-98db-78a776c84552',5,'从 1 到 100 的整数中，有多少个能被 2、3、5 至少一个整除？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7e15192d-6a52-5351-b3ae-a635fcab618d','74。','容斥：50+33+20−16−10−6+3=74。两两交集按最小公倍数计数，再加回三者交集。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d7adcf5e-7542-5411-b84d-17914bd28b3c','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','8411d829-0a5e-5862-98db-78a776c84552',2,'集合与计数 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d7adcf5e-7542-5411-b84d-17914bd28b3c','例：5 人中选 2 人，不分顺序有 C(5,2)=10 种。','例：5 人中选 2 人，不分顺序有 C(5,2)=10 种。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('d98fd99f-6eae-5907-bd1c-443d73369eec','ccd819bb-a48b-5c75-bfbf-e26219023fef','relations');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','d98fd99f-6eae-5907-bd1c-443d73369eec','关系、等价关系与偏序','自反：∀a，aRa；对称：aRb ⇒ bRa；反对称：aRb 且 bRa ⇒ a=b；传递：aRb 且 bRc ⇒ aRc。
等价关系=自反+对称+传递，可把集合划分为互不相交的等价类。
偏序=自反+反对称+传递，不要求任意两元素可比。
易错：反对称不是“不对称”，关系可以同时对称与反对称。',3,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('f7b709f5-2f0b-54d5-9301-dd2bab80cb4c','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','d98fd99f-6eae-5907-bd1c-443d73369eec',1,'整数的等号关系是否自反？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('f7b709f5-2f0b-54d5-9301-dd2bab80cb4c','是。','每个整数等于自身。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('46b5a435-1e27-511f-b8a3-89ad73812dc1','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','d98fd99f-6eae-5907-bd1c-443d73369eec',3,'整数上的 aRb 当且仅当 a−b 为 3 的倍数，是等价关系吗？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('46b5a435-1e27-511f-b8a3-89ad73812dc1','是，分成三个余数类。','0 是 3 的倍数；倍数取负仍是倍数；两个倍数相加仍是倍数，分别验证自反、对称、传递。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('64d5babc-0d0f-5723-895f-689a2e871859','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','d98fd99f-6eae-5907-bd1c-443d73369eec',5,'在 {1,2,3,6} 上按整除排序，求 2、3 的最小上界与最大下界。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('64d5babc-0d0f-5723-895f-689a2e871859','最小上界 6，最大下界 1。','共同上界需同时被 2、3 整除，集合中只有 6；共同下界需同时整除两者，只有 1。普通数值大小与整除偏序不同。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5a60b124-e403-50e7-b588-ea3ecfccf447','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','d98fd99f-6eae-5907-bd1c-443d73369eec',3,'关系、等价关系与偏序 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5a60b124-e403-50e7-b588-ea3ecfccf447','例：整数上“模 3 同余”是等价关系；集合间“包含”是偏序。','例：整数上“模 3 同余”是等价关系；集合间“包含”是偏序。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('32179bc0-eeaf-52fd-a847-0e983b6803ae','ccd819bb-a48b-5c75-bfbf-e26219023fef','functions');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','32179bc0-eeaf-52fd-a847-0e983b6803ae','函数、单射、满射与双射','单射：不同输入的输出不同；满射：陪域中每个元素都有原像；双射：既单又满。
双射才存在由陪域到定义域的逆函数。
复合：(g∘f)(x)=g(f(x))，先做 f。
判断性质时必须同时明确函数的定义域和陪域。',2,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('3c8aaeb6-08e0-5d5c-8028-8a103a7a4b78','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','32179bc0-eeaf-52fd-a847-0e983b6803ae',1,'实数域上 f(x)=x+1 是否双射？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('3c8aaeb6-08e0-5d5c-8028-8a103a7a4b78','是。','相同输出推出相同输入；对任意 y，取 x=y−1 有原像。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7c7ba631-cbd8-5591-ac1b-ac2eb184629a','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','32179bc0-eeaf-52fd-a847-0e983b6803ae',3,'整数到整数的 f(x)=2x 是什么类型？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7c7ba631-cbd8-5591-ac1b-ac2eb184629a','单射但非满射。','2a=2b 推出 a=b；奇数无原像。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a0d9c73a-6c90-5d18-bb6d-b4634a5cb003','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','32179bc0-eeaf-52fd-a847-0e983b6803ae',5,'若 g∘f 为双射，能否推出 f、g 均为双射？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a0d9c73a-6c90-5d18-bb6d-b4634a5cb003','不能，只能必然推出 f 单射、g 满射。','反例：f 从 {1} 映到 {1,2}，f(1)=1；g 将 {1,2} 都映到 {1}。复合为单点双射，但 f 非满、g 非单。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b6246a51-26eb-5779-b875-4000f933f3b9','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','32179bc0-eeaf-52fd-a847-0e983b6803ae',2,'函数、单射、满射与双射 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b6246a51-26eb-5779-b875-4000f933f3b9','例：f:ℤ→ℤ，f(x)=2x，为单射但不是满射，因为奇数无原像。','例：f:ℤ→ℤ，f(x)=2x，为单射但不是满射，因为奇数无原像。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('e59da350-f044-5045-9aca-d911225fbae9','ccd819bb-a48b-5c75-bfbf-e26219023fef','algebra');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','e59da350-f044-5045-9aca-d911225fbae9','代数结构与群','半群：集合上的二元运算封闭且满足结合律。幺半群：半群再有单位元。
群：幺半群中每个元素都有逆元；交换群另外满足交换律。
有限群中，子群的阶整除群的阶（拉格朗日定理）。
易错：证明群不能只验证封闭性，要检查结合律、单位元、逆元。',3,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('1975988c-9fa3-57f6-8b2e-d937ecc6d966','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','e59da350-f044-5045-9aca-d911225fbae9',1,'整数加法群的单位元是什么？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('1975988c-9fa3-57f6-8b2e-d937ecc6d966','0。','任意 x 都满足 x+0=0+x=x。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('27529fdc-7179-5aa0-a377-0442b846c81e','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','e59da350-f044-5045-9aca-d911225fbae9',3,'整数乘法是否构成群？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('27529fdc-7179-5aa0-a377-0442b846c81e','不是。','如 2 无整数乘法逆元，0 更无逆元。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7ffb7162-55e4-51e3-8cb1-baba03fb073f','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','e59da350-f044-5045-9aca-d911225fbae9',5,'模 8 乘法下的 {1,3,5,7} 是循环群吗？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7ffb7162-55e4-51e3-8cb1-baba03fb073f','是群，但不是循环群。','各元素平方模 8 都为 1，乘法封闭且继承结合律，逆元为自身。非单位元素阶均为 2，没有阶 4 的生成元。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ebaf0f81-fecf-513e-8489-e691d99843af','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','e59da350-f044-5045-9aca-d911225fbae9',3,'代数结构与群 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ebaf0f81-fecf-513e-8489-e691d99843af','例：整数在加法下构成交换群，单位元 0，x 的逆元 −x。整数在乘法下不是群。','例：整数在加法下构成交换群，单位元 0，x 的逆元 −x。整数在乘法下不是群。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('a3361a46-719a-5062-a3dd-610f638c8c47','ccd819bb-a48b-5c75-bfbf-e26219023fef','graphs');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('9fa5e36d-08a0-5370-855e-2d2cd3c3d545','a3361a46-719a-5062-a3dd-610f638c8c47','图、树与遍历','无向图握手定理：∑deg(v)=2|E|，奇度顶点个数为偶数。
树是连通无环无向图；n 个顶点的树有 n−1 条边。
有限连通无向图有欧拉回路当且仅当所有顶点度数为偶数；恰有两个奇度点时有非闭合欧拉迹。
BFS 按层扩展；DFS 沿分支深入。邻接表下两者时间复杂度 O(V+E)。
易错：欧拉问题关注边，哈密顿问题关注顶点。',3,'[]','[{"kind":"LINK","label":"MIT · Mathematics for Computer Science","url":"https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-spring-2015/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9b40895f-1c02-5e75-98e7-5643f4a302fd','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','a3361a46-719a-5062-a3dd-610f638c8c47',1,'8 个顶点的树有几条边？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9b40895f-1c02-5e75-98e7-5643f4a302fd','7。','树的边数为顶点数减 1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e0d6078e-7932-567f-ac42-a34b0de9f7e1','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','a3361a46-719a-5062-a3dd-610f638c8c47',3,'连通无向图恰有两个奇度顶点，有欧拉回路吗？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e0d6078e-7932-567f-ac42-a34b0de9f7e1','没有闭合欧拉回路，但有从一个奇度点到另一个的欧拉迹。','闭合回路要求所有度数为偶数；恰有两个奇度点时它们是开迹端点。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c8555701-c771-5231-b564-ee2e516afbf0','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','a3361a46-719a-5062-a3dd-610f638c8c47',5,'证明 n≥2 的有限树至少有两个叶子。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c8555701-c771-5231-b564-ee2e516afbf0','至少两个度数为 1 的顶点。','取最长简单路径。若其任一端点有路径外邻点就可延长；若有路径内非相邻邻点会成环。故两个端点均只与路径上的下一点相邻，都是叶子。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a79f91cc-4e8f-5b79-b9b5-364869e64690','9fa5e36d-08a0-5370-855e-2d2cd3c3d545','a3361a46-719a-5062-a3dd-610f638c8c47',3,'图、树与遍历 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a79f91cc-4e8f-5b79-b9b5-364869e64690','例：5 个顶点的树有 4 条边。','例：5 个顶点的树有 4 条边。');
UPDATE content_release SET state='RETIRED' WHERE id='bc3169f1-12a5-5863-9c9a-325258124830';
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','4c6db663-13a2-5b2b-8cfa-6422a6c37205',2,'PUBLISHED',UTC_TIMESTAMP(6),'5c8aebe64846841e86daf127a3f2e2fa00f8f55546e04d037b35d276ed75f1d3');
INSERT INTO chapter_revision SELECT 'cfbaa40a-50cb-5d78-b24f-3867315e6585',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='a6fed138-39df-5495-bacf-a654e48c3561';
INSERT INTO item_revision SELECT 'cfbaa40a-50cb-5d78-b24f-3867315e6585',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='a6fed138-39df-5495-bacf-a654e48c3561';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('e6a9b3d3-3880-53ea-bedc-9f67eba3b25c','4c6db663-13a2-5b2b-8cfa-6422a6c37205','performance');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','e6a9b3d3-3880-53ea-bedc-9f67eba3b25c','性能、CPI 与加速比','CPU 时间 = 指令数 IC × 平均 CPI / 时钟频率 f。频率单位 Hz，每秒周期数。
平均 CPI = ∑(各类指令比例 × 对应 CPI)。
加速比=改进前时间/改进后时间。Amdahl：总加速比=1/[(1−p)+p/s]，p 为原执行时间中可优化比例，s 为局部加速比。
易错：主频更高不必然程序更快，还受指令数、CPI 影响。',2,'[]','[{"kind":"LINK","label":"MIT · Computation Structures","url":"https://www.ocw.mit.edu/courses/6-004-computation-structures-spring-2009/resources/lecture-notes/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9ac7cb59-a32e-5ae4-8ccb-43443f4024a3','cfbaa40a-50cb-5d78-b24f-3867315e6585','e6a9b3d3-3880-53ea-bedc-9f67eba3b25c',1,'10⁹ 条指令，CPI=2，频率 2GHz，CPU 时间多少？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9ac7cb59-a32e-5ae4-8ccb-43443f4024a3','1 秒。','时间=10⁹×2/(2×10⁹)。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('67bd7950-dee7-5367-b337-f53b816bbeb4','cfbaa40a-50cb-5d78-b24f-3867315e6585','e6a9b3d3-3880-53ea-bedc-9f67eba3b25c',3,'80% 执行时间可加速为 4 倍，总加速比多少？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('67bd7950-dee7-5367-b337-f53b816bbeb4','2.5。','Amdahl：1/(0.2+0.8/4)=2.5。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('816ae0fd-dec5-593f-a5f2-cf8e0c245c2e','cfbaa40a-50cb-5d78-b24f-3867315e6585','e6a9b3d3-3880-53ea-bedc-9f67eba3b25c',5,'原程序占比 p 的部分加速 5 倍，要使总加速比至少 3，p 至少多少？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('816ae0fd-dec5-593f-a5f2-cf8e0c245c2e','5/6，约 83.33%。','由 1/(1−p+p/5)≥3，分母为正，解得 1−4p/5≤1/3，即 p≥5/6。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a6e03873-06e1-57a3-a9cd-1803db04cd47','cfbaa40a-50cb-5d78-b24f-3867315e6585','e6a9b3d3-3880-53ea-bedc-9f67eba3b25c',2,'性能、CPI 与加速比 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a6e03873-06e1-57a3-a9cd-1803db04cd47','例：IC=10⁹，CPI=2，f=2 GHz，则 CPU 时间=1 秒。','例：IC=10⁹，CPI=2，f=2 GHz，则 CPU 时间=1 秒。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('a6539ff1-8de7-5d10-9435-0c85e10da176','4c6db663-13a2-5b2b-8cfa-6422a6c37205','binary');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','a6539ff1-8de7-5d10-9435-0c85e10da176','数制、补码与溢出','n 位无符号整数范围：0 至 2ⁿ−1；n 位补码有符号整数范围：−2ⁿ⁻¹ 至 2ⁿ⁻¹−1。
负数补码：对应正数二进制逐位取反再加 1（固定字长）。
有符号加法：同号相加得到异号结果时溢出。进位标志与有符号溢出标志含义不同。
浮点数由符号、指数和尾数组成；有效数字位数限制导致舍入误差。',2,'[]','[{"kind":"LINK","label":"MIT · Computation Structures","url":"https://www.ocw.mit.edu/courses/6-004-computation-structures-spring-2009/resources/lecture-notes/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('297a64a5-1c73-59c0-af96-f7d5d33d2234','cfbaa40a-50cb-5d78-b24f-3867315e6585','a6539ff1-8de7-5d10-9435-0c85e10da176',1,'8 位补码最小值是多少？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('297a64a5-1c73-59c0-af96-f7d5d33d2234','−128。','范围 −2⁷ 至 2⁷−1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('eabd8e78-6843-5556-b311-f9af753a0c2d','cfbaa40a-50cb-5d78-b24f-3867315e6585','a6539ff1-8de7-5d10-9435-0c85e10da176',3,'8 位补码表示 −5。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('eabd8e78-6843-5556-b311-f9af753a0c2d','11111011。','00000101 取反加 1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c3bd6bdb-86b0-5e34-93e3-91d9bbd87932','cfbaa40a-50cb-5d78-b24f-3867315e6585','a6539ff1-8de7-5d10-9435-0c85e10da176',5,'8 位补码中 100+60 的位模式和溢出标志如何？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c3bd6bdb-86b0-5e34-93e3-91d9bbd87932','10100000，按补码解释为 −96；有符号溢出。','真值 160 超过 127；模 256 保留 160 的位模式。同号正数相加得到负号，溢出；不要将模结果当真值。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('6ee1744e-3acd-5584-a593-7206bce16651','cfbaa40a-50cb-5d78-b24f-3867315e6585','a6539ff1-8de7-5d10-9435-0c85e10da176',2,'数制、补码与溢出 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('6ee1744e-3acd-5584-a593-7206bce16651','例：8 位 +5=00000101，−5=11111011。','例：8 位 +5=00000101，−5=11111011。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46','4c6db663-13a2-5b2b-8cfa-6422a6c37205','cache');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46','存储层次与 Cache','局部性：时间局部性指近期访问的内容可能再访问；空间局部性指邻近地址可能被访问。
单级 Cache 平均访问时间 AMAT=命中时间+缺失率×缺失额外代价。
直接映射：内存块号 mod Cache 行数决定行号；组相联：块号 mod 组数决定组号。
地址划分：标记、组索引、块内偏移。块大小 2ᵇ 字节时偏移占 b 位。',3,'[]','[{"kind":"LINK","label":"MIT · Computation Structures","url":"https://www.ocw.mit.edu/courses/6-004-computation-structures-spring-2009/resources/lecture-notes/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('01bb7f1e-a1f9-5116-bbf1-950658c8e9ef','cfbaa40a-50cb-5d78-b24f-3867315e6585','4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46',1,'64 字节块的块内偏移多少位？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('01bb7f1e-a1f9-5116-bbf1-950658c8e9ef','6 位。','64=2⁶。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d261ba62-f01a-5399-9b5d-5531f837d445','cfbaa40a-50cb-5d78-b24f-3867315e6585','4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46',3,'命中时间 1ns，缺失率 5%，额外代价 100ns，AMAT 多少？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d261ba62-f01a-5399-9b5d-5531f837d445','6ns。','1+0.05×100。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('228fa2aa-7d37-5c8c-a971-d69fb5d6d587','cfbaa40a-50cb-5d78-b24f-3867315e6585','4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46',5,'32 位地址，32KiB 数据容量，4 路组相联，块 64B，不计元数据，求标记/组索引/偏移位数。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('228fa2aa-7d37-5c8c-a971-d69fb5d6d587','19 / 7 / 6 位。','总行数 32768/64=512，组数 512/4=128，索引 7 位、偏移 6 位，标记为 32−7−6=19。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('495872c0-36fb-5197-8584-c1e6e68fbfdb','cfbaa40a-50cb-5d78-b24f-3867315e6585','4cbde3ed-5cea-5a3b-a7f9-ac2001fa7e46',3,'存储层次与 Cache · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('495872c0-36fb-5197-8584-c1e6e68fbfdb','例：命中 1 ns，缺失率 5%，额外代价 100 ns，AMAT=6 ns。','例：命中 1 ns，缺失率 5%，额外代价 100 ns，AMAT=6 ns。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('79e5a74e-cefb-5d36-b327-9b6ac955ea33','4c6db663-13a2-5b2b-8cfa-6422a6c37205','cpu');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','79e5a74e-cefb-5d36-b327-9b6ac955ea33','指令执行、流水线与冒险','典型指令周期包括取指、译码、执行、访存、写回，实际划分依体系结构而定。
k 段等时长理想流水线执行 n 条指令约用 (k+n−1) 个周期；不考虑冒险和额外开销。
结构冒险：资源冲突；数据冒险：依赖未满足；控制冒险：分支改变执行流。
转发可缓解部分数据冒险，不能消除所有等待。',3,'[]','[{"kind":"LINK","label":"MIT · Computation Structures","url":"https://www.ocw.mit.edu/courses/6-004-computation-structures-spring-2009/resources/lecture-notes/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e26fe44b-6d3c-5bba-bd52-2fbeede0b599','cfbaa40a-50cb-5d78-b24f-3867315e6585','79e5a74e-cefb-5d36-b327-9b6ac955ea33',1,'五段理想流水线执行一条指令需要几周期？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e26fe44b-6d3c-5bba-bd52-2fbeede0b599','5 周期。','没有其他指令重叠时仍需流过全部阶段。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('498fab5d-2133-5181-a716-38d38845e4c7','cfbaa40a-50cb-5d78-b24f-3867315e6585','79e5a74e-cefb-5d36-b327-9b6ac955ea33',3,'五段等长流水线执行 10 条指令，理想用时多少周期？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('498fab5d-2133-5181-a716-38d38845e4c7','14 周期。','k+n−1=5+10−1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c5102311-6e66-55d2-9644-c44c3ea91420','cfbaa40a-50cb-5d78-b24f-3867315e6585','79e5a74e-cefb-5d36-b327-9b6ac955ea33',5,'五段流水线 100 条指令，20 次独立停顿事件，每次额外停 2 周期，无其他开销。相比 500 周期非流水，加速比多少？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c5102311-6e66-55d2-9644-c44c3ea91420','约 3.47。','理想 104 周期，停顿增加 40，共 144；500/144≈3.47。确认停顿不重叠是题设前提。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7b51001b-0f8c-5e2f-80c5-c8c0ede0572b','cfbaa40a-50cb-5d78-b24f-3867315e6585','79e5a74e-cefb-5d36-b327-9b6ac955ea33',3,'指令执行、流水线与冒险 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7b51001b-0f8c-5e2f-80c5-c8c0ede0572b','例：5 段流水线执行 10 条指令，理想需 14 周期；非流水为 50 个同长度阶段周期。','例：5 段流水线执行 10 条指令，理想需 14 周期；非流水为 50 个同长度阶段周期。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('63308fae-17d0-55b4-8eb3-838770f95e2d','4c6db663-13a2-5b2b-8cfa-6422a6c37205','io');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('cfbaa40a-50cb-5d78-b24f-3867315e6585','63308fae-17d0-55b4-8eb3-838770f95e2d','中断、DMA 与虚拟存储','程序查询由 CPU 反复检查设备；中断方式由设备请求 CPU 响应；DMA 允许控制器直接参与内存与设备间的数据传送，减少逐字 CPU 搬运。
分页把虚拟地址拆成页号和页内偏移，页表映射到物理页框。TLB 缓存地址转换。
页大小 4 KiB 时页内偏移为 12 位。缺页需操作系统处理，不等同于 Cache 未命中。
易错：DMA 不表示完全不需要 CPU，初始化和完成处理仍需参与。',3,'[]','[{"kind":"LINK","label":"MIT · Computation Structures","url":"https://www.ocw.mit.edu/courses/6-004-computation-structures-spring-2009/resources/lecture-notes/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7744b46e-b167-5262-868a-222d9edd5401','cfbaa40a-50cb-5d78-b24f-3867315e6585','63308fae-17d0-55b4-8eb3-838770f95e2d',1,'4KiB 页的页内偏移多少位？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7744b46e-b167-5262-868a-222d9edd5401','12 位。','4096=2¹²。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('3cc98c80-0b5c-54c9-8608-d3787abec58e','cfbaa40a-50cb-5d78-b24f-3867315e6585','63308fae-17d0-55b4-8eb3-838770f95e2d',3,'DMA 是否意味着完全不需要 CPU？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('3cc98c80-0b5c-54c9-8608-d3787abec58e','不是。','CPU 负责初始化与完成处理，数据搬运主要交给 DMA 控制器。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('774ec7ba-00c3-513e-b941-1a304529465a','cfbaa40a-50cb-5d78-b24f-3867315e6585','63308fae-17d0-55b4-8eb3-838770f95e2d',5,'TLB 查询 10ns，内存访问 100ns，命中率 90%；未命中只需一次额外页表访问，无缺页，不并行。平均访问时间？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('774ec7ba-00c3-513e-b941-1a304529465a','120ns。','命中 110ns，未命中 210ns；0.9×110+0.1×210=120。缺页代价未计入，不能与 TLB 未命中混淆。');
UPDATE content_release SET state='RETIRED' WHERE id='a6fed138-39df-5495-bacf-a654e48c3561';
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('2d6a538a-931a-5a34-801c-1f3ceac3ec26','72494a28-2cb8-5df8-8f3f-2ba84b681e0d',2,'PUBLISHED',UTC_TIMESTAMP(6),'b90b37d767cd8f0aec26d69b8183ab62f9e1fecbea7ed6f25a9d813ea2a7a378');
INSERT INTO chapter_revision SELECT '2d6a538a-931a-5a34-801c-1f3ceac3ec26',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='7926ce74-94bb-579c-972e-a5b36034a839';
INSERT INTO item_revision SELECT '2d6a538a-931a-5a34-801c-1f3ceac3ec26',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='7926ce74-94bb-579c-972e-a5b36034a839';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('6139674c-3a38-50d7-9f7e-4c7d28c55e6a','72494a28-2cb8-5df8-8f3f-2ba84b681e0d','query');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a','SELECT、筛选与分组','常见结构：SELECT 列 FROM 表 WHERE 行条件 GROUP BY 分组列 HAVING 组条件 ORDER BY 排序列。
WHERE 在分组前筛选行，HAVING 筛选分组结果。COUNT(*) 数行，COUNT(列) 忽略该列 NULL。
NULL 用 IS NULL 或 IS NOT NULL 判断，不用 =NULL。
SQL 执行的逻辑处理顺序不同于书写顺序；避免选取未分组且未聚合的任意列。',2,'[]','[{"kind":"LINK","label":"MySQL 官方查询文档","url":"https://dev.mysql.com/doc/refman/8.4/en/select.html","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d0c91911-cfc5-52b0-a17c-211ed4350cda','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'判断列为空应写 =NULL 吗？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d0c91911-cfc5-52b0-a17c-211ed4350cda','写 IS NULL。','NULL 参与普通等号比较产生未知，不等于真。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('94b8f0b7-02e9-5c50-960c-be41926f260c','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',3,'统计各部门平均成绩并仅保留均分≥60 的部门。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('94b8f0b7-02e9-5c50-960c-be41926f260c','SELECT dept, AVG(score) FROM marks GROUP BY dept HAVING AVG(score)>=60;','GROUP BY 先分组，HAVING 过滤聚合结果；WHERE 不用于直接过滤该聚合值。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('139bf975-e88c-5cff-a1d1-fba9e04e2b7e','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',5,'表 t(x) 含 1、NULL、1，求 COUNT(*)、COUNT(x)、COUNT(DISTINCT x)。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('139bf975-e88c-5cff-a1d1-fba9e04e2b7e','3、2、1。','第一项计所有行；第二项排除 NULL；第三项先按非空值去重再计数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c9b26c56-3977-5d3b-9541-83da94c65739','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',2,'SELECT、筛选与分组 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c9b26c56-3977-5d3b-9541-83da94c65739','例：SELECT dept, AVG(score) AS avg_score FROM marks GROUP BY dept HAVING AVG(score)>=60;','例：SELECT dept, AVG(score) AS avg_score FROM marks GROUP BY dept HAVING AVG(score)>=60;');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('2a5dac43-ac02-5a0b-8072-fbaa5bb6626b','72494a28-2cb8-5df8-8f3f-2ba84b681e0d','joins');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('2d6a538a-931a-5a34-801c-1f3ceac3ec26','2a5dac43-ac02-5a0b-8072-fbaa5bb6626b','连接、子查询与约束','INNER JOIN 保留匹配行；LEFT JOIN 保留左表全部行，右侧无匹配时补 NULL。
主键唯一且非空；外键维护参照关系；UNIQUE、NOT NULL、CHECK 分别约束唯一性、非空与条件。
易错：对 LEFT JOIN 右表列在 WHERE 中排除 NULL，可能使结果表现为内连接。
EXISTS 判断子查询是否存在行，适合表达“至少有一条关联记录”。',3,'[]','[{"kind":"LINK","label":"MySQL 官方查询文档","url":"https://dev.mysql.com/doc/refman/8.4/en/select.html","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('1b5c3e85-2cb4-5117-a5be-68b6bb69799b','2d6a538a-931a-5a34-801c-1f3ceac3ec26','2a5dac43-ac02-5a0b-8072-fbaa5bb6626b',1,'LEFT JOIN 会保留左表无匹配的行吗？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('1b5c3e85-2cb4-5117-a5be-68b6bb69799b','会。','右侧字段以 NULL 补齐。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('10778f57-b6e6-5bcf-9ceb-693e454bcd8b','2d6a538a-931a-5a34-801c-1f3ceac3ec26','2a5dac43-ac02-5a0b-8072-fbaa5bb6626b',3,'如何查找没有选课记录的学生？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('10778f57-b6e6-5bcf-9ceb-693e454bcd8b','SELECT s.id FROM student s WHERE NOT EXISTS (SELECT 1 FROM enroll e WHERE e.student_id=s.id);','相关子查询按学生 ID 找关联记录，NOT EXISTS 保留零记录学生。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('7c813eeb-0a6f-5342-b319-9826fab5983d','2d6a538a-931a-5a34-801c-1f3ceac3ec26','2a5dac43-ac02-5a0b-8072-fbaa5bb6626b',5,'要列出所有学生及其≥60分成绩，为何 LEFT JOIN 后 WHERE e.score>=60 会漏人？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('7c813eeb-0a6f-5342-b319-9826fab5983d','WHERE 排除了右侧为 NULL 的行。','把 e.score>=60 放在 ON 中：LEFT JOIN enroll e ON e.student_id=s.id AND e.score>=60。左表所有学生得以保留；两种写法业务语义不同。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ba2831b6-ade0-5770-ab2e-9d6c9ac77450','2d6a538a-931a-5a34-801c-1f3ceac3ec26','2a5dac43-ac02-5a0b-8072-fbaa5bb6626b',3,'连接、子查询与约束 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ba2831b6-ade0-5770-ab2e-9d6c9ac77450','例：SELECT s.name,c.title FROM student s LEFT JOIN course c ON s.course_id=c.id;','例：SELECT s.name,c.title FROM student s LEFT JOIN course c ON s.course_id=c.id;');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('d78a5051-3c16-57d4-a318-aca00db803f9','72494a28-2cb8-5df8-8f3f-2ba84b681e0d','transactions');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('2d6a538a-931a-5a34-801c-1f3ceac3ec26','d78a5051-3c16-57d4-a318-aca00db803f9','事务、索引与操作顺序','ACID：原子性、一致性、隔离性、持久性。
常见事务流程：START TRANSACTION; 更新操作; COMMIT; 出错时 ROLLBACK;。具体能力取决于存储引擎和语句。
索引可加速某些读取，但增加写入维护和存储成本；不是越多越好。
实践检查：确认数据库和表 → 先 SELECT 验证范围 → 再 UPDATE/DELETE → 校验结果。
易错：部分 DDL 会隐式提交，不能假定所有语句都能回滚。',3,'[]','[{"kind":"LINK","label":"MySQL 官方查询文档","url":"https://dev.mysql.com/doc/refman/8.4/en/select.html","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a2e02ffd-ac94-5f3a-bc2c-1646e6e033d9','2d6a538a-931a-5a34-801c-1f3ceac3ec26','d78a5051-3c16-57d4-a318-aca00db803f9',1,'事务的原子性含义是什么？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a2e02ffd-ac94-5f3a-bc2c-1646e6e033d9','同一事务的操作整体成功或整体撤销。','不能只完成转账扣款而不完成入账。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('51cc93a4-e72b-5e83-a881-a685cf247414','2d6a538a-931a-5a34-801c-1f3ceac3ec26','d78a5051-3c16-57d4-a318-aca00db803f9',3,'为什么索引不是越多越好？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('51cc93a4-e72b-5e83-a881-a685cf247414','每次写入还要维护索引，并消耗空间。','根据筛选、连接、排序模式和写入负载评估实际收益。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c6a2f2b9-9ead-5396-a990-1d81067aba6c','2d6a538a-931a-5a34-801c-1f3ceac3ec26','d78a5051-3c16-57d4-a318-aca00db803f9',5,'并发转账时仅先查余额再扣款有什么问题？如何改进？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c6a2f2b9-9ead-5396-a990-1d81067aba6c','存在竞争与超额扣款风险。','在支持事务的引擎中，使用行锁或带余额条件的原子 UPDATE，检查影响行数，并在同一事务内入账；失败回滚，统一锁顺序降低死锁风险。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ba8e58cc-c952-56ad-be46-cf5edf00ef1f','2d6a538a-931a-5a34-801c-1f3ceac3ec26','d78a5051-3c16-57d4-a318-aca00db803f9',3,'事务、索引与操作顺序 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ba8e58cc-c952-56ad-be46-cf5edf00ef1f','例：UPDATE student SET score=80 WHERE id=1; WHERE 决定更新范围。','例：UPDATE student SET score=80 WHERE id=1; WHERE 决定更新范围。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ee0ed6cc-dc1a-5d6f-83ce-28506d3f15ac','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建数据库 tempexam，字符集设置为 utf8。
选择 tempexam，创建表 book，包含图书编号、书名、价格和出版日期。
为 book 添加 publisher 字段。
把 publisher 的长度修改为 100。
把 publisher 改名为 press。
复制 book 的表结构，生成 bookbackup。
把 bookbackup 重命名为 oldbook。
查看 book 的表结构和建表语句。
删除 oldbook，最后删除 tempexam 数据库。',2004);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ee0ed6cc-dc1a-5d6f-83ce-28506d3f15ac','```sql
CREATE DATABASE temp_exam DEFAULT CHARACTER SET utf8;
USE temp_exam;

CREATE TABLE book (
  book_id INT PRIMARY KEY,
  book_name VARCHAR(50) NOT NULL,
  price DECIMAL(8,2),
  publish_date DATE
);

ALTER TABLE book ADD COLUMN publisher VARCHAR(50);
ALTER TABLE book MODIFY COLUMN publisher VARCHAR(100);
ALTER TABLE book CHANGE COLUMN publisher press VARCHAR(100);
CREATE TABLE book_backup LIKE book;
ALTER TABLE book_backup RENAME TO old_book;
DESC book;
SHOW CREATE TABLE book;
DROP TABLE old_book;
DROP DATABASE temp_exam;
USE exam_practice;
```','```sql
CREATE DATABASE temp_exam DEFAULT CHARACTER SET utf8;
USE temp_exam;

CREATE TABLE book (
  book_id INT PRIMARY KEY,
  book_name VARCHAR(50) NOT NULL,
  price DECIMAL(8,2),
  publish_date DATE
);

ALTER TABLE book ADD COLUMN publisher VARCHAR(50);
ALTER TABLE book MODIFY COLUMN publisher VARCHAR(100);
ALTER TABLE book CHANGE COLUMN publisher press VARCHAR(100);
CREATE TABLE book_backup LIKE book;
ALTER TABLE book_backup RENAME TO old_book;
DESC book;
SHOW CREATE TABLE book;
DROP TABLE old_book;
DROP DATABASE temp_exam;
USE exam_practice;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('188df3ec-38d1-5e27-b413-203cf7c42edb','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'向 student 添加学生：学号 20260009、姓名 吴桐、女、计算机 1 班。
把吴桐的电话号码修改为 13800000009。
给数据库原理课程的所有成绩增加 2 分，但最高不能超过 100 分。
删除吴桐。由于没有成绩记录，应该可以直接删除。
复制 student 表的结构与数据，生成 studentcopy。
删除 studentcopy 中网络 1 班的学生。
删除 studentcopy。',2005);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('188df3ec-38d1-5e27-b413-203cf7c42edb','```sql
USE exam_practice;

INSERT INTO student
  (student_no, student_name, gender, class_name)
VALUES
  (''20260009'', ''吴桐'', ''女'', ''计算机1班'');

SELECT * FROM student WHERE student_no = ''20260009'';

UPDATE student
SET phone = ''13800000009''
WHERE student_no = ''20260009'';

SELECT * FROM score WHERE course_id = 1;

UPDATE score
SET score = LEAST(score + 2, 100)
WHERE course_id = 1;

DELETE FROM student
WHERE student_no = ''20260009'';

CREATE TABLE student_copy AS
SELECT * FROM student;

SELECT * FROM student_copy WHERE class_name = ''网络1班'';

DELETE FROM student_copy
WHERE class_name = ''网络1班'';

DROP TABLE student_copy;
```

执行本节第 3 题会修改练习数据。继续其他章节前，可以重新执行第 3 章初始化脚本恢复原始成绩。','```sql
USE exam_practice;

INSERT INTO student
  (student_no, student_name, gender, class_name)
VALUES
  (''20260009'', ''吴桐'', ''女'', ''计算机1班'');

SELECT * FROM student WHERE student_no = ''20260009'';

UPDATE student
SET phone = ''13800000009''
WHERE student_no = ''20260009'';

SELECT * FROM score WHERE course_id = 1;

UPDATE score
SET score = LEAST(score + 2, 100)
WHERE course_id = 1;

DELETE FROM student
WHERE student_no = ''20260009'';

CREATE TABLE student_copy AS
SELECT * FROM student;

SELECT * FROM student_copy WHERE class_name = ''网络1班'';

DELETE FROM student_copy
WHERE class_name = ''网络1班'';

DROP TABLE student_copy;
```

执行本节第 3 题会修改练习数据。继续其他章节前，可以重新执行第 3 章初始化脚本恢复原始成绩。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('23982a5e-fc88-521e-8e78-1e1ff05a3215','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'查询计算机 1 班的所有学生。
查询所有女生的姓名和班级。
查询姓名中包含“明”的学生。
查询电话号码为空的学生。
查询出生日期在 2001-01-01 到 2001-12-31 之间的学生。
查询所有成绩，按成绩从高到低排列，只显示前 5 条。
查询一共有多少名学生。
查询成绩的最高分、最低分和平均分。
按课程编号统计选课人数和平均分。
查询平均分不低于 80 分的课程编号。
统计每个班级的学生人数，并按人数降序排列。
查询不重复的班级名称。',2006);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('23982a5e-fc88-521e-8e78-1e1ff05a3215','```sql
SELECT *
FROM student
WHERE class_name = ''计算机1班'';

SELECT student_name, class_name
FROM student
WHERE gender = ''女'';

SELECT *
FROM student
WHERE student_name LIKE ''%明%'';

SELECT *
FROM student
WHERE phone IS NULL;

SELECT *
FROM student
WHERE birth_date BETWEEN ''2001-01-01'' AND ''2001-12-31'';

SELECT *
FROM score
ORDER BY score DESC
LIMIT 5;

SELECT COUNT(*) AS student_count
FROM student;

SELECT
  MAX(score) AS max_score,
  MIN(score) AS min_score,
  AVG(score) AS avg_score
FROM score;

SELECT
  course_id,
  COUNT(*) AS student_count,
  ROUND(AVG(score), 2) AS avg_score
FROM score
GROUP BY course_id;

SELECT course_id, ROUND(AVG(score), 2) AS avg_score
FROM score
GROUP BY course_id
HAVING AVG(score) >= 80;

SELECT class_name, COUNT(*) AS student_count
FROM student
GROUP BY class_name
ORDER BY student_count DESC;

SELECT DISTINCT class_name
FROM student;
```','```sql
SELECT *
FROM student
WHERE class_name = ''计算机1班'';

SELECT student_name, class_name
FROM student
WHERE gender = ''女'';

SELECT *
FROM student
WHERE student_name LIKE ''%明%'';

SELECT *
FROM student
WHERE phone IS NULL;

SELECT *
FROM student
WHERE birth_date BETWEEN ''2001-01-01'' AND ''2001-12-31'';

SELECT *
FROM score
ORDER BY score DESC
LIMIT 5;

SELECT COUNT(*) AS student_count
FROM student;

SELECT
  MAX(score) AS max_score,
  MIN(score) AS min_score,
  AVG(score) AS avg_score
FROM score;

SELECT
  course_id,
  COUNT(*) AS student_count,
  ROUND(AVG(score), 2) AS avg_score
FROM score
GROUP BY course_id;

SELECT course_id, ROUND(AVG(score), 2) AS avg_score
FROM score
GROUP BY course_id
HAVING AVG(score) >= 80;

SELECT class_name, COUNT(*) AS student_count
FROM student
GROUP BY class_name
ORDER BY student_count DESC;

SELECT DISTINCT class_name
FROM student;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('2ec43e54-454d-5528-a6af-ff0aa9e09a17','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'查询学生姓名、课程名称和成绩。
查询张明的全部课程名称和成绩。
查询数据库原理课程的学生姓名和成绩，按成绩降序排列。
查询所有学生及其平均分，没有成绩的学生也必须显示。
查询从未被选择的课程。
查询高于全部成绩平均分的成绩记录。
查询数据库原理课程中高于本课程平均分的学生。
查询至少有一门课程不及格的学生。
查询没有任何成绩记录的学生。
使用 UNION 合并计算机 1 班与网络 1 班学生的姓名和班级。',2007);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('2ec43e54-454d-5528-a6af-ff0aa9e09a17','```sql
SELECT s.student_name, c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id;

SELECT c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE s.student_name = ''张明'';

SELECT s.student_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE c.course_name = ''数据库原理''
ORDER BY sc.score DESC;

SELECT
  s.student_id,
  s.student_name,
  ROUND(AVG(sc.score), 2) AS avg_score
FROM student AS s
LEFT JOIN score AS sc ON s.student_id = sc.student_id
GROUP BY s.student_id, s.student_name;

SELECT c.*
FROM course AS c
LEFT JOIN score AS sc ON c.course_id = sc.course_id
WHERE sc.course_id IS NULL;

SELECT *
FROM score
WHERE score > (SELECT AVG(score) FROM score);

SELECT s.student_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE c.course_name = ''数据库原理''
  AND sc.score > (
    SELECT AVG(sc2.score)
    FROM score AS sc2
    INNER JOIN course AS c2 ON sc2.course_id = c2.course_id
    WHERE c2.course_name = ''数据库原理''
  );

SELECT DISTINCT s.student_id, s.student_name
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
WHERE sc.score < 60;

SELECT *
FROM student
WHERE student_id NOT IN (
  SELECT student_id FROM score
);

SELECT student_name, class_name
FROM student
WHERE class_name = ''计算机1班''
UNION
SELECT student_name, class_name
FROM student
WHERE class_name = ''网络1班'';
```','```sql
SELECT s.student_name, c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id;

SELECT c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE s.student_name = ''张明'';

SELECT s.student_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE c.course_name = ''数据库原理''
ORDER BY sc.score DESC;

SELECT
  s.student_id,
  s.student_name,
  ROUND(AVG(sc.score), 2) AS avg_score
FROM student AS s
LEFT JOIN score AS sc ON s.student_id = sc.student_id
GROUP BY s.student_id, s.student_name;

SELECT c.*
FROM course AS c
LEFT JOIN score AS sc ON c.course_id = sc.course_id
WHERE sc.course_id IS NULL;

SELECT *
FROM score
WHERE score > (SELECT AVG(score) FROM score);

SELECT s.student_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id
WHERE c.course_name = ''数据库原理''
  AND sc.score > (
    SELECT AVG(sc2.score)
    FROM score AS sc2
    INNER JOIN course AS c2 ON sc2.course_id = c2.course_id
    WHERE c2.course_name = ''数据库原理''
  );

SELECT DISTINCT s.student_id, s.student_name
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
WHERE sc.score < 60;

SELECT *
FROM student
WHERE student_id NOT IN (
  SELECT student_id FROM score
);

SELECT student_name, class_name
FROM student
WHERE class_name = ''计算机1班''
UNION
SELECT student_name, class_name
FROM student
WHERE class_name = ''网络1班'';
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d344373d-39b5-586a-9619-02fa25ec2e07','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建 department 表，包含部门编号主键和唯一的部门名称。
给 student 添加 departmentid 字段。
为该字段添加名称为 fkstudentdepartment 的外键。
查看 student 完整建表语句。
删除该外键，再删除字段和 department 表。',2008);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d344373d-39b5-586a-9619-02fa25ec2e07','```sql
CREATE TABLE department (
  department_id INT PRIMARY KEY,
  department_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

ALTER TABLE student
ADD COLUMN department_id INT;

ALTER TABLE student
ADD CONSTRAINT fk_student_department
FOREIGN KEY (department_id)
REFERENCES department(department_id);

SHOW CREATE TABLE student;

ALTER TABLE student
DROP FOREIGN KEY fk_student_department;

ALTER TABLE student
DROP COLUMN department_id;

DROP TABLE department;
```','```sql
CREATE TABLE department (
  department_id INT PRIMARY KEY,
  department_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

ALTER TABLE student
ADD COLUMN department_id INT;

ALTER TABLE student
ADD CONSTRAINT fk_student_department
FOREIGN KEY (department_id)
REFERENCES department(department_id);

SHOW CREATE TABLE student;

ALTER TABLE student
DROP FOREIGN KEY fk_student_department;

ALTER TABLE student
DROP COLUMN department_id;

DROP TABLE department;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('37d04aa1-12c1-52dc-91e9-5a69f300b2d7','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'在 student 的姓名字段上创建普通索引 idxstudentname。
在班级和性别上创建联合索引 idxclassgender。
查看 student 的全部索引。
删除刚创建的两个索引。',2009);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('37d04aa1-12c1-52dc-91e9-5a69f300b2d7','```sql
CREATE INDEX idx_student_name
ON student(student_name);

CREATE INDEX idx_class_gender
ON student(class_name, gender);

SHOW INDEX FROM student;

DROP INDEX idx_student_name ON student;
DROP INDEX idx_class_gender ON student;
```','```sql
CREATE INDEX idx_student_name
ON student(student_name);

CREATE INDEX idx_class_gender
ON student(class_name, gender);

SHOW INDEX FROM student;

DROP INDEX idx_student_name ON student;
DROP INDEX idx_class_gender ON student;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b8956f52-71ae-5332-bc1c-3c075ee716c6','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建视图 vstudentscore，显示学生姓名、课程名称和成绩。
查询该视图中不及格的记录。
创建简单视图 vcomputer1student，显示计算机 1 班学生。
通过简单视图修改张明的电话号码。
把 vcomputer1student 修改为只显示学号、姓名和电话。
查看视图定义，最后删除两个视图。',2010);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b8956f52-71ae-5332-bc1c-3c075ee716c6','```sql
CREATE VIEW v_student_score AS
SELECT s.student_name, c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id;

SELECT *
FROM v_student_score
WHERE score < 60;

CREATE VIEW v_computer1_student AS
SELECT *
FROM student
WHERE class_name = ''计算机1班'';

UPDATE v_computer1_student
SET phone = ''13900000001''
WHERE student_no = ''20260001'';

CREATE OR REPLACE VIEW v_computer1_student AS
SELECT student_no, student_name, phone
FROM student
WHERE class_name = ''计算机1班'';

SHOW CREATE VIEW v_computer1_student;

DROP VIEW v_student_score;
DROP VIEW v_computer1_student;
```','```sql
CREATE VIEW v_student_score AS
SELECT s.student_name, c.course_name, sc.score
FROM student AS s
INNER JOIN score AS sc ON s.student_id = sc.student_id
INNER JOIN course AS c ON sc.course_id = c.course_id;

SELECT *
FROM v_student_score
WHERE score < 60;

CREATE VIEW v_computer1_student AS
SELECT *
FROM student
WHERE class_name = ''计算机1班'';

UPDATE v_computer1_student
SET phone = ''13900000001''
WHERE student_no = ''20260001'';

CREATE OR REPLACE VIEW v_computer1_student AS
SELECT student_no, student_name, phone
FROM student
WHERE class_name = ''计算机1班'';

SHOW CREATE VIEW v_computer1_student;

DROP VIEW v_student_score;
DROP VIEW v_computer1_student;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('010b8497-5a29-593a-9f44-9f39bca4aab8','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建删除成绩后的日志触发器。
删除该触发器。
说明 BEFORE 和 AFTER 的区别。',2011);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('010b8497-5a29-593a-9f44-9f39bca4aab8','```sql
DELIMITER $$

CREATE TRIGGER trg_score_after_delete
AFTER DELETE ON score
FOR EACH ROW
BEGIN
  INSERT INTO operation_log(action_type, detail)
  VALUES (
    ''DELETE'',
    CONCAT(
      ''删除成绩：学生ID='', OLD.student_id,
      '',课程ID='', OLD.course_id,
      '',成绩='', OLD.score
    )
  );
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS trg_score_after_delete;
```

- `BEFORE` 在数据写入前执行，常用于检查或调整数据。
- `AFTER` 在数据写入成功后执行，常用于记录日志或执行后续操作。','```sql
DELIMITER $$

CREATE TRIGGER trg_score_after_delete
AFTER DELETE ON score
FOR EACH ROW
BEGIN
  INSERT INTO operation_log(action_type, detail)
  VALUES (
    ''DELETE'',
    CONCAT(
      ''删除成绩：学生ID='', OLD.student_id,
      '',课程ID='', OLD.course_id,
      '',成绩='', OLD.score
    )
  );
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS trg_score_after_delete;
```

- `BEFORE` 在数据写入前执行，常用于检查或调整数据。
- `AFTER` 在数据写入成功后执行，常用于记录日志或执行后续操作。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('dcb4962f-6036-53b1-b6ab-618255b6444d','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建过程 pcoursestatistics，传入课程编号，返回最高分、最低分和平均分。
查看当前数据库的存储过程。
删除三个练习过程。',2012);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('dcb4962f-6036-53b1-b6ab-618255b6444d','```sql
DELIMITER $$

CREATE PROCEDURE p_course_statistics(IN p_course_id INT)
BEGIN
  SELECT
    MAX(score) AS max_score,
    MIN(score) AS min_score,
    ROUND(AVG(score), 2) AS avg_score
  FROM score
  WHERE course_id = p_course_id;
END$$

DELIMITER ;

CALL p_course_statistics(1);

SHOW PROCEDURE STATUS
WHERE Db = ''exam_practice'';

DROP PROCEDURE IF EXISTS p_query_score;
DROP PROCEDURE IF EXISTS p_student_average;
DROP PROCEDURE IF EXISTS p_course_statistics;
```','```sql
DELIMITER $$

CREATE PROCEDURE p_course_statistics(IN p_course_id INT)
BEGIN
  SELECT
    MAX(score) AS max_score,
    MIN(score) AS min_score,
    ROUND(AVG(score), 2) AS avg_score
  FROM score
  WHERE course_id = p_course_id;
END$$

DELIMITER ;

CALL p_course_statistics(1);

SHOW PROCEDURE STATUS
WHERE Db = ''exam_practice'';

DROP PROCEDURE IF EXISTS p_query_score;
DROP PROCEDURE IF EXISTS p_student_average;
DROP PROCEDURE IF EXISTS p_course_statistics;
```');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5152e6a3-226b-5383-b2c8-acce37907f70','2d6a538a-931a-5a34-801c-1f3ceac3ec26','6139674c-3a38-50d7-9f7e-4c7d28c55e6a',1,'创建数据库 mockexam，字符集为 utf8，并选择该数据库。
创建部门表 department：部门编号为主键，部门名称非空且唯一。
创建员工表 employee，至少包含：
插入 3 个部门和至少 6 名员工。
给员工表添加电话字段，然后把它修改为 VARCHAR(30)。
复制员工表结构为 employeebackup。
把工资低于 5000 的员工工资增加 500。
查询工资在 5000～8000 之间的员工，按工资降序排列。
查询每个部门的人数、最高工资和平均工资。
查询平均工资大于 6000 的部门。
使用连接查询员工姓名、部门名称和工资。
查询工资高于全体员工平均工资的员工。
查询没有员工的部门。
使用 UNION 合并两个指定部门的员工姓名。
在员工姓名上创建索引并查看索引。
创建员工信息视图，显示员工姓名、部门名称和工资。
创建存储过程，根据部门编号查询该部门全部员工。
创建一个员工工资修改后记录日志的触发器。
写出创建用户并授予 mockexam 查询权限的 SQL。
写出备份 mockexam 和恢复到 mockrestore 的命令。',2018);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5152e6a3-226b-5383-b2c8-acce37907f70','```sql
CREATE DATABASE mock_exam DEFAULT CHARACTER SET utf8;
USE mock_exam;

CREATE TABLE department (
  department_id INT PRIMARY KEY,
  department_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE employee (
  employee_id INT PRIMARY KEY AUTO_INCREMENT,
  employee_name VARCHAR(30) NOT NULL,
  gender ENUM(''男'', ''女'') NOT NULL DEFAULT ''男'',
  salary DECIMAL(10,2),
  hire_date DATE,
  department_id INT,
  CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id)
    REFERENCES department(department_id)
) ENGINE=InnoDB;

INSERT INTO department VALUES
  (1, ''技术部''),
  (2, ''财务部''),
  (3, ''行政部'');

INSERT INTO employee
  (employee_name, gender, salary, hire_date, department_id)
VALUES
  (''张明'', ''男'', 6800, ''2022-03-01'', 1),
  (''李华'', ''女'', 8200, ''2021-06-15'', 1),
  (''王强'', ''男'', 4800, ''2023-02-10'', 1),
  (''赵敏'', ''女'', 7200, ''2020-09-20'', 2),
  (''陈晨'', ''女'', 5500, ''2024-01-08'', 2),
  (''刘洋'', ''男'', 4300, ''2023-11-11'', NULL);

ALTER TABLE employee ADD COLUMN phone VARCHAR(20);
ALTER TABLE employee MODIFY COLUMN phone VARCHAR(30);
CREATE TABLE employee_backup LIKE employee;

UPDATE employee
SET salary = salary + 500
WHERE salary < 5000;

SELECT *
FROM employee
WHERE salary BETWEEN 5000 AND 8000
ORDER BY salary DESC;

SELECT
  department_id,
  COUNT(*) AS employee_count,
  MAX(salary) AS max_salary,
  ROUND(AVG(salary), 2) AS avg_salary
FROM employee
GROUP BY department_id;

SELECT department_id, ROUND(AVG(salary), 2) AS avg_salary
FROM employee
GROUP BY department_id
HAVING AVG(salary) > 6000;

SELECT e.employee_name, d.department_name, e.salary
FROM employee AS e
INNER JOIN department AS d
  ON e.department_id = d.department_id;

SELECT *
FROM employee
WHERE salary > (SELECT AVG(salary) FROM employee);

SELECT d.*
FROM department AS d
LEFT JOIN employee AS e
  ON d.department_id = e.department_id
WHERE e.employee_id IS NULL;

SELECT employee_name
FROM employee
WHERE department_id = 1
UNION
SELECT employee_name
FROM employee
WHERE department_id = 2;

CREATE INDEX idx_employee_name
ON employee(employee_name);

SHOW INDEX FROM employee;

CREATE VIEW v_employee_info AS
SELECT e.employee_name, d.department_name, e.salary
FROM employee AS e
LEFT JOIN department AS d
  ON e.department_id = d.department_id;

DELIMITER $$

CREATE PROCEDURE p_department_employee(IN p_department_id INT)
BEGIN
  SELECT *
  FROM employee
  WHERE department_id = p_department_id;
END$$

DELIMITER ;

CREATE TABLE salary_log (
  log_id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  old_salary DECIMAL(10,2),
  new_salary DECIMAL(10,2),
  change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$

CREATE TRIGGER trg_employee_salary_update
AFTER UPDATE ON employee
FOR EACH ROW
BEGIN
  IF OLD.salary <> NEW.salary THEN
    INSERT INTO salary_log(employee_id, old_salary, new_salary)
    VALUES (OLD.employee_id, OLD.salary, NEW.salary);
  END IF;
END$$

DELIMITER ;

CREATE USER ''mock_user''@''localhost''
IDENTIFIED BY ''Mock123!'';

GRANT SELECT
ON mock_exam.*
TO ''mock_user''@''localhost'';
```

命令提示符中的备份与恢复：

```bat
mysqldump -u root -p mock_exam > mock_exam.sql
mysql -u root -p -e "CREATE DATABASE mock_restore DEFAULT CHARACTER SET utf8;"
mysql -u root -p mock_restore < mock_exam.sql
```','```sql
CREATE DATABASE mock_exam DEFAULT CHARACTER SET utf8;
USE mock_exam;

CREATE TABLE department (
  department_id INT PRIMARY KEY,
  department_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE employee (
  employee_id INT PRIMARY KEY AUTO_INCREMENT,
  employee_name VARCHAR(30) NOT NULL,
  gender ENUM(''男'', ''女'') NOT NULL DEFAULT ''男'',
  salary DECIMAL(10,2),
  hire_date DATE,
  department_id INT,
  CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id)
    REFERENCES department(department_id)
) ENGINE=InnoDB;

INSERT INTO department VALUES
  (1, ''技术部''),
  (2, ''财务部''),
  (3, ''行政部'');

INSERT INTO employee
  (employee_name, gender, salary, hire_date, department_id)
VALUES
  (''张明'', ''男'', 6800, ''2022-03-01'', 1),
  (''李华'', ''女'', 8200, ''2021-06-15'', 1),
  (''王强'', ''男'', 4800, ''2023-02-10'', 1),
  (''赵敏'', ''女'', 7200, ''2020-09-20'', 2),
  (''陈晨'', ''女'', 5500, ''2024-01-08'', 2),
  (''刘洋'', ''男'', 4300, ''2023-11-11'', NULL);

ALTER TABLE employee ADD COLUMN phone VARCHAR(20);
ALTER TABLE employee MODIFY COLUMN phone VARCHAR(30);
CREATE TABLE employee_backup LIKE employee;

UPDATE employee
SET salary = salary + 500
WHERE salary < 5000;

SELECT *
FROM employee
WHERE salary BETWEEN 5000 AND 8000
ORDER BY salary DESC;

SELECT
  department_id,
  COUNT(*) AS employee_count,
  MAX(salary) AS max_salary,
  ROUND(AVG(salary), 2) AS avg_salary
FROM employee
GROUP BY department_id;

SELECT department_id, ROUND(AVG(salary), 2) AS avg_salary
FROM employee
GROUP BY department_id
HAVING AVG(salary) > 6000;

SELECT e.employee_name, d.department_name, e.salary
FROM employee AS e
INNER JOIN department AS d
  ON e.department_id = d.department_id;

SELECT *
FROM employee
WHERE salary > (SELECT AVG(salary) FROM employee);

SELECT d.*
FROM department AS d
LEFT JOIN employee AS e
  ON d.department_id = e.department_id
WHERE e.employee_id IS NULL;

SELECT employee_name
FROM employee
WHERE department_id = 1
UNION
SELECT employee_name
FROM employee
WHERE department_id = 2;

CREATE INDEX idx_employee_name
ON employee(employee_name);

SHOW INDEX FROM employee;

CREATE VIEW v_employee_info AS
SELECT e.employee_name, d.department_name, e.salary
FROM employee AS e
LEFT JOIN department AS d
  ON e.department_id = d.department_id;

DELIMITER $$

CREATE PROCEDURE p_department_employee(IN p_department_id INT)
BEGIN
  SELECT *
  FROM employee
  WHERE department_id = p_department_id;
END$$

DELIMITER ;

CREATE TABLE salary_log (
  log_id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  old_salary DECIMAL(10,2),
  new_salary DECIMAL(10,2),
  change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$

CREATE TRIGGER trg_employee_salary_update
AFTER UPDATE ON employee
FOR EACH ROW
BEGIN
  IF OLD.salary <> NEW.salary THEN
    INSERT INTO salary_log(employee_id, old_salary, new_salary)
    VALUES (OLD.employee_id, OLD.salary, NEW.salary);
  END IF;
END$$

DELIMITER ;

CREATE USER ''mock_user''@''localhost''
IDENTIFIED BY ''Mock123!'';

GRANT SELECT
ON mock_exam.*
TO ''mock_user''@''localhost'';
```

命令提示符中的备份与恢复：

```bat
mysqldump -u root -p mock_exam > mock_exam.sql
mysql -u root -p -e "CREATE DATABASE mock_restore DEFAULT CHARACTER SET utf8;"
mysql -u root -p mock_restore < mock_exam.sql
```');
INSERT INTO stored_file (id,owner_id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,contains_answers,state) VALUES ('6644cbe1-f788-5d30-b27f-45e0eb6f4850',NULL,'MANUAL','classpath:manuals/database-v2.json','database-manual.json','application/json',43011,'89f4627c3034ea6a40abca6dd430474d0a92555dc1c0e00d2c08679a66849078',0,'ACTIVE');
UPDATE module_revision SET resources=JSON_ARRAY_APPEND(resources,'$',CAST('{"kind":"FILE","label":"13171 数据库及其应用（实践）应试练习册","url":null,"fileId":"6644cbe1-f788-5d30-b27f-45e0eb6f4850"}' AS JSON)) WHERE release_id='2d6a538a-931a-5a34-801c-1f3ceac3ec26' AND module_id='6139674c-3a38-50d7-9f7e-4c7d28c55e6a';
UPDATE content_release SET state='RETIRED' WHERE id='7926ce74-94bb-579c-972e-a5b36034a839';
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('5374723b-643e-5870-8da4-a694a5ee7a5c','e2dd5f58-a12f-55e0-a691-455ea2233699',2,'PUBLISHED',UTC_TIMESTAMP(6),'8500fd17bd9d8c10c829a2c8ce2cc5b4c1d77352e4511f6472c5a5417124785f');
INSERT INTO chapter_revision SELECT '5374723b-643e-5870-8da4-a694a5ee7a5c',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='c7fd8161-dbbc-5d50-9b15-be6eefc63ead';
INSERT INTO item_revision SELECT '5374723b-643e-5870-8da4-a694a5ee7a5c',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='c7fd8161-dbbc-5d50-9b15-be6eefc63ead';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('e02711c3-5cbc-543a-b956-93ed764b61c1','e2dd5f58-a12f-55e0-a691-455ea2233699','basics');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1','Java 类型、运算与控制流','基本类型包括 byte、short、int、long、float、double、char、boolean；String 是引用类型。
整数除法会截断小数：5/2=2，5/2.0=2.5。
字符串内容比较使用 equals；== 对对象比较引用是否相同。
for(int i=0;i<a.length;i++) 遍历数组，最后下标为 length−1。
易错：数组越界、整数溢出、把 = 当作条件比较。',1,'[]','[{"kind":"LINK","label":"Java 官方学习文档","url":"https://dev.java/learn/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('772fe745-a2b2-5a6d-84cf-45cdb2d86073','5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1',1,'Java 的 5/2 与 5/2.0 分别是多少？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('772fe745-a2b2-5a6d-84cf-45cdb2d86073','2 和 2.5。','前者整数除法，后者提升为浮点计算。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e6316d05-362c-5f3e-b383-4d68e08ba24c','5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1',3,'如何比较两个非空 String 的内容？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e6316d05-362c-5f3e-b383-4d68e08ba24c','使用 a.equals(b)。','== 判断是否同一引用；若 a 可能为 null，可用 Objects.equals(a,b)。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a1a72d62-64e1-5561-8b10-0ddbd24116d7','5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1',5,'对 int 数组求和，为什么把最终结果强制转 long 仍可能错误？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a1a72d62-64e1-5561-8b10-0ddbd24116d7','溢出可能已在 int 累加时发生。','应从一开始用 long sum=0; 再逐项累加，避免先以 int 运算再转换。测试 Integer.MAX_VALUE 与 1 相加的边界。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('de5c82db-2e62-59e3-8724-ad911c35a31a','5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1',1,'Java 类型、运算与控制流 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('de5c82db-2e62-59e3-8724-ad911c35a31a','例：int sum=0; for(int x:a){sum+=x;} 求和。','例：int sum=0; for(int x:a){sum+=x;} 求和。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('6a2f6c96-379c-5da5-bb80-c67adfa6b08d','e2dd5f58-a12f-55e0-a691-455ea2233699','oop');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('5374723b-643e-5870-8da4-a694a5ee7a5c','6a2f6c96-379c-5da5-bb80-c67adfa6b08d','类、继承与多态','封装：把状态与操作组织进类，通过访问控制维护约束。继承表达 is-a 关系；组合表达 has-a 关系。
重载：同名方法参数列表不同；重写：子类提供兼容签名的实例方法实现。
多态：父类引用可指向子类对象，实例方法调用通常根据实际对象动态分派。
构造方法无返回类型；static 属于类，不依赖某个实例。',2,'[]','[{"kind":"LINK","label":"Java 官方学习文档","url":"https://dev.java/learn/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c0bde519-7123-51c7-8abd-edf16a0117d9','5374723b-643e-5870-8da4-a694a5ee7a5c','6a2f6c96-379c-5da5-bb80-c67adfa6b08d',1,'构造方法有返回类型吗？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c0bde519-7123-51c7-8abd-edf16a0117d9','没有。','名称与类名相同；写 void 就成了普通方法。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('db710d3b-ba03-5b68-a45f-7c3cd2143614','5374723b-643e-5870-8da4-a694a5ee7a5c','6a2f6c96-379c-5da5-bb80-c67adfa6b08d',3,'父类引用指向子类对象，调用被重写的实例方法时执行哪个版本？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('db710d3b-ba03-5b68-a45f-7c3cd2143614','实际子类对象的重写版本。','实例方法动态分派，区别于 static 隐藏。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9951a1ad-8fc4-587d-99db-1cd6db6fa309','5374723b-643e-5870-8da4-a694a5ee7a5c','6a2f6c96-379c-5da5-bb80-c67adfa6b08d',5,'父类有 static f 和实例 g，子类分别隐藏 f、重写 g。Parent p=new Child(); p.f(); p.g(); 调用谁？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9951a1ad-8fc4-587d-99db-1cd6db6fa309','父类 f、子类 g。','静态调用依编译期引用类型，实例调用依运行时对象。更推荐用类名调用 static，避免误读。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a7539aec-9096-5afc-9cc1-9a77434886ae','5374723b-643e-5870-8da4-a694a5ee7a5c','6a2f6c96-379c-5da5-bb80-c67adfa6b08d',2,'类、继承与多态 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a7539aec-9096-5afc-9cc1-9a77434886ae','例：Animal a=new Dog(); a.speak(); 调用 Dog 重写的 speak。','例：Animal a=new Dog(); a.speak(); 调用 Dog 重写的 speak。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('3d55154c-0eed-5199-bfae-1d592cb90a6d','e2dd5f58-a12f-55e0-a691-455ea2233699','practice');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('5374723b-643e-5870-8da4-a694a5ee7a5c','3d55154c-0eed-5199-bfae-1d592cb90a6d','异常、集合与上机检查','try/catch 捕获异常，finally 用于清理；可关闭资源优先使用 try-with-resources。
List 保留顺序并允许重复；Set 表达不重复集合；Map 存储键值映射。
上机步骤：读输入规格 → 明确边界 → 写最小实现 → 编译 → 用正常、空值和边界数据验证。
排序或查找题先确认是否允许调用库函数。
界面事件应避免长时间阻塞 Swing 事件线程；历史 Applet 内容按指定教材复习，不作为现代浏览器部署方案。',3,'[]','[{"kind":"LINK","label":"Java 官方学习文档","url":"https://dev.java/learn/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d2de5f26-bd17-517a-9d0b-6d24a4558053','5374723b-643e-5870-8da4-a694a5ee7a5c','3d55154c-0eed-5199-bfae-1d592cb90a6d',1,'保存不重复元素使用 List 还是 Set？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d2de5f26-bd17-517a-9d0b-6d24a4558053','Set。','List 允许重复，Set 以其相等性规则去重。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('8397cbff-d784-5ccd-a983-985ed289a06b','5374723b-643e-5870-8da4-a694a5ee7a5c','3d55154c-0eed-5199-bfae-1d592cb90a6d',3,'如何统计字符串每个字符的次数？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('8397cbff-d784-5ccd-a983-985ed289a06b','使用 Map<Character,Integer> 累计。','逐个字符执行 map.merge(c,1,Integer::sum)；注意 char 按 UTF-16 代码单元处理，若要完整 Unicode 字符需按 code point。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('8e736d92-d26c-5f1f-8073-18ea5d43a2d8','5374723b-643e-5870-8da4-a694a5ee7a5c','3d55154c-0eed-5199-bfae-1d592cb90a6d',5,'两个线程对共享 int 执行 count++，仅加 volatile 可以保证最终次数正确吗？',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('8e736d92-d26c-5f1f-8073-18ea5d43a2d8','不能。','count++ 是读、加、写复合操作，volatile 不使整体原子化。用 AtomicInteger.incrementAndGet 或锁保护；同时测试多线程及异常路径。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('597d41fd-d105-5ee0-bd7b-baa72c709187','5374723b-643e-5870-8da4-a694a5ee7a5c','3d55154c-0eed-5199-bfae-1d592cb90a6d',3,'异常、集合与上机检查 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('597d41fd-d105-5ee0-bd7b-baa72c709187','例：字符串统计可用 Map<Character,Integer>，更新计数时处理首次出现。','例：字符串统计可用 Map<Character,Integer>，更新计数时处理首次出现。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b3cf574b-335f-519b-8512-9355f9407c36','5374723b-643e-5870-8da4-a694a5ee7a5c','e02711c3-5cbc-543a-b956-93ed764b61c1',1,'统计英文字母、数字、空格和其他字符的数量；
输出其中所有数字字符；
如果没有数字，输出“没有数字字符”。',2056);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b3cf574b-335f-519b-8512-9355f9407c36','```text
输入：Java 2026!
字母：4
数字：4
空格：1
其他：1
数字字符：2026
```','```text
输入：Java 2026!
字母：4
数字：4
空格：1
其他：1
数字字符：2026
```');
INSERT INTO stored_file (id,owner_id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,contains_answers,state) VALUES ('f11c1efb-dd7c-5b5e-bbbf-ec4757c69e20',NULL,'MANUAL','classpath:manuals/java-v2.json','java-manual.json','application/json',44067,'d0ea4a073a519ab6af0e2e4488e1a7ab809f5424bfd6140f16537533d58a4d4a',0,'ACTIVE');
UPDATE module_revision SET resources=JSON_ARRAY_APPEND(resources,'$',CAST('{"kind":"FILE","label":"13216 Java语言程序设计（实践）学习与练习手册","url":null,"fileId":"f11c1efb-dd7c-5b5e-bbbf-ec4757c69e20"}' AS JSON)) WHERE release_id='5374723b-643e-5870-8da4-a694a5ee7a5c' AND module_id='e02711c3-5cbc-543a-b956-93ed764b61c1';
UPDATE content_release SET state='RETIRED' WHERE id='c7fd8161-dbbc-5d50-9b15-be6eefc63ead';
INSERT INTO content_release (id,course_id,version_no,state,published_at,source_sha) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','6450a55a-7d59-5632-b9ad-2a4e170af220',2,'PUBLISHED',UTC_TIMESTAMP(6),'45ac386706bcc95aade058bac18c58cfa96f925af2846c74dd230fe38fb3616e');
INSERT INTO chapter_revision SELECT '619c8138-c56a-5ade-8fb8-c042b4c21ee1',chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id='10a804de-4d52-55c6-a677-62783fa5de09';
INSERT INTO item_revision SELECT '619c8138-c56a-5ade-8fb8-c042b4c21ee1',item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id='10a804de-4d52-55c6-a677-62783fa5de09';
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('87716b4e-3768-58f4-bcec-0f9c09cf7c5e','6450a55a-7d59-5632-b9ad-2a4e170af220','determinants');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e','行列式','二阶：|a b; c d|=ad−bc。
交换两行，行列式变号；某行乘 k，行列式乘 k；某行加另一行的 k 倍，值不变。
三角矩阵行列式等于对角线元素乘积。det(AB)=det(A)det(B)，det(Aᵀ)=det(A)。
n 阶方阵可逆 ⇔ det(A)≠0。
易错：行列式是数，矩阵是一个数组对象。',2,'[{"label":"二阶行列式","tex":"\\\\begin{vmatrix}a&b\\\\\\\\c&d\\\\end{vmatrix}=ad-bc","condition":"行列式为标量"},{"label":"乘积与可逆","tex":"\\\\det(AB)=\\\\det A\\\\det B,\\\\quad A\\\\text{ 可逆}\\\\iff\\\\det A\\\\ne0","condition":"A、B 为同阶方阵"}]','[{"kind":"LINK","label":"MIT · Linear Algebra","url":"https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('430e7069-b039-5acd-aa12-157447fd69fd','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',1,'计算 $\\begin{vmatrix}1&2\\\\3&4\\end{vmatrix}$。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('430e7069-b039-5acd-aa12-157447fd69fd','$-2$','主对角乘积减副对角乘积：4−6。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('cb07c644-22e9-5b6a-ae50-a08c0a24a649','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',2,'一个三阶矩阵行列式为 2，交换两行后再将一行乘 3，新行列式是多少？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('cb07c644-22e9-5b6a-ae50-a08c0a24a649','$-6$','交换行变号，单行乘 3 使行列式乘 3。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('68f4f0c3-fd29-53e8-9c8f-72b7e171c5f4','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',3,'$A$ 为三阶矩阵，$\\det A=2$，求 $\\det(2A)$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('68f4f0c3-fd29-53e8-9c8f-72b7e171c5f4','$16$','三行均乘 2，所以乘 $2^3$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('930b976f-58fa-58c5-b710-0116826ec8e2','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',4,'求 $A=\\begin{pmatrix}a&1&1\\\\1&a&1\\\\1&1&a\\end{pmatrix}$ 的行列式。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('930b976f-58fa-58c5-b710-0116826ec8e2','$(a-1)^2(a+2)$','写成 $(a-1)I+J$，J 为全 1 矩阵，特征值 3、0、0，所以 A 的特征值为 a+2、a−1、a−1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('17c8a522-e19e-5c55-afa6-8517b4e11926','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',5,'设 $A$ 为四阶可逆矩阵，$\\det A=2$，$A^*$ 为伴随矩阵，求 $\\det(3A^{-1}A^*)$。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('17c8a522-e19e-5c55-afa6-8517b4e11926','$324$','由 $A^*=2A^{-1}$ 得乘积为 $6A^{-2}$。行列式为 $6^4(\\det A)^{-2}=1296/4=324$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('9998d492-5655-5f57-8ee3-00b8325bcd03','619c8138-c56a-5ade-8fb8-c042b4c21ee1','87716b4e-3768-58f4-bcec-0f9c09cf7c5e',2,'行列式 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('9998d492-5655-5f57-8ee3-00b8325bcd03','例：A=[[1,2],[3,4]]，det(A)=4−6=−2。','例：A=[[1,2],[3,4]]，det(A)=4−6=−2。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('8754d88b-4a90-5f49-bdfe-14e86c01cc1f','6450a55a-7d59-5632-b9ad-2a4e170af220','matrices');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f','矩阵运算与逆矩阵','矩阵乘法要求左列数=右行数；(AB)ᵢⱼ=∑aᵢₖbₖⱼ。通常 AB≠BA。
(AB)ᵀ=BᵀAᵀ；(AB)⁻¹=B⁻¹A⁻¹（均可逆时）。
二阶逆矩阵：A⁻¹=(1/(ad−bc))[[d,−b],[−c,a]]。
求逆通法：对增广矩阵 [A|I] 行变换至 [I|A⁻¹]。
易错：矩阵“除法”不能直接照搬实数除法，左右乘逆矩阵结果可能不同。',2,'[{"label":"逆矩阵","tex":"\\\\begin{pmatrix}a&b\\\\\\\\c&d\\\\end{pmatrix}^{-1}=\\\\frac1{ad-bc}\\\\begin{pmatrix}d&-b\\\\\\\\-c&a\\\\end{pmatrix}","condition":"ad−bc≠0"},{"label":"乘积的逆","tex":"(AB)^{-1}=B^{-1}A^{-1}","condition":"两矩阵均可逆，顺序不能颠倒"}]','[{"kind":"LINK","label":"MIT · Linear Algebra","url":"https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('56f568ae-18be-539f-8dcd-21a9c2fd018f','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',1,'求 $\\operatorname{diag}(2,4)$ 的逆。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('56f568ae-18be-539f-8dcd-21a9c2fd018f','$\\operatorname{diag}(1/2,1/4)$','对角元素取倒数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('20f86a00-61db-5768-8d1b-e6834deed62a','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',2,'若 A 是 $2\\times3$，B 是 $3\\times4$，AB 的尺寸是多少？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('20f86a00-61db-5768-8d1b-e6834deed62a','$2\\times4$','中间维数匹配，输出保留左矩阵行数和右矩阵列数。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c36cbf35-749e-5a67-8b52-a741d42586ac','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',3,'求 $\\begin{pmatrix}1&1\\\\0&1\\end{pmatrix}^{-1}$。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c36cbf35-749e-5a67-8b52-a741d42586ac','$\\begin{pmatrix}1&-1\\\\0&1\\end{pmatrix}$','行列式为 1，代入二阶求逆公式。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a72fec83-a86f-53de-b1c0-e6507629c25f','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',4,'$A$ 可逆，解矩阵方程 $AX+2A=B$。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a72fec83-a86f-53de-b1c0-e6507629c25f','$X=A^{-1}B-2I$','两边左乘 A⁻¹，不能随意改为右乘。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('3c1ae02d-edad-5549-8d94-e7e767234006','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',5,'若方阵 $A$ 满足 $A^2-3A+2I=0$，求 $A^{-1}$ 并将 $A^n$ 表为 A 与 I 的线性组合（$n\\ge0$）。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('3c1ae02d-edad-5549-8d94-e7e767234006','$A^{-1}=(3I-A)/2$；$A^n=(2^n-1)A+(2-2^n)I$。','由 $A(3I-A)=2I$ 得逆。定义投影 $P=2I-A,Q=A-I$，验证 P+Q=I、PQ=0、P²=P、Q²=Q，且 A=P+2Q。因此 $A^n=P+2^nQ$。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('e778bbe8-604c-51f8-95a5-aad6578de5d1','619c8138-c56a-5ade-8fb8-c042b4c21ee1','8754d88b-4a90-5f49-bdfe-14e86c01cc1f',2,'矩阵运算与逆矩阵 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('e778bbe8-604c-51f8-95a5-aad6578de5d1','例：diag(2,3) 的逆为 diag(1/2,1/3)。','例：diag(2,3) 的逆为 diag(1/2,1/3)。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('c6871ac3-eee2-5a4b-8486-ddc53c93d5be','6450a55a-7d59-5632-b9ad-2a4e170af220','rank');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be','秩、线性相关与方程组','矩阵秩等于行阶梯形中非零行数，也等于最大非零子式阶数。
向量组线性相关：存在不全为 0 的系数使线性组合为 0。
Ax=b 有解 ⇔ r(A)=r([A|b])。有解且秩=n（未知量数）时唯一解；秩<n 时有自由变量。
Ax=0 的解空间维数为 n−r(A)；r(A)<n 才有非零解。',3,'[{"label":"有解判定","tex":"Ax=b\\\\text{ 有解}\\\\iff r(A)=r([A\\\\mid b])","condition":"n 个未知量时，秩为 n 则唯一解"},{"label":"零空间维数","tex":"\\\\dim\\\\ker A=n-r(A)","condition":"A 有 n 列"}]','[{"kind":"LINK","label":"MIT · Linear Algebra","url":"https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('dc4835d1-5f76-566c-b3d4-7ae1c5ed0585','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',1,'零矩阵的秩是多少？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('dc4835d1-5f76-566c-b3d4-7ae1c5ed0585','$0$','没有非零行也没有非零子式。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('0182c29c-9c9e-5e41-adf1-2836452d549c','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',2,'求 $\\begin{pmatrix}1&2\\\\2&4\\end{pmatrix}$ 的秩。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('0182c29c-9c9e-5e41-adf1-2836452d549c','$1$','第二行是第一行的两倍，第一行非零。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('0a1dca55-8b17-5d4b-b539-ff9a56bf34cc','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',3,'解 $x+y+z=0$ 并写基础解系。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('0a1dca55-8b17-5d4b-b539-ff9a56bf34cc','$(x,y,z)=s(-1,1,0)+t(-1,0,1)$。','令 y=s,z=t，得到 x=−s−t，两参数对应两个基向量。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5781822c-07a9-543f-afe1-1263c37e0091','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',4,'某有解方程组 5 个未知数且系数矩阵秩为 3，通解需要几个自由参数？',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5781822c-07a9-543f-afe1-1263c37e0091','$2$','解集合为特解加零空间，零空间维数为 5−3。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('b5bb08a8-f338-535d-9bb9-debf67c953ab','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',5,'讨论方程组 $x+y+z=1,\\ x+ay+z=1,\\ x+y+az=b$ 的解。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('b5bb08a8-f338-535d-9bb9-debf67c953ab','a≠1 时唯一解 $(\\frac{a-b}{a-1},0,\\frac{b-1}{a-1})$；a=1,b=1 时无穷多解；a=1,b≠1 时无解。','第二、三式分别减第一式得 (a−1)y=0、(a−1)z=b−1。分 a=1 与否讨论，退化情形需比较常数列，不能直接除以 a−1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('a7351894-5883-5fff-b850-b55d026f42df','619c8138-c56a-5ade-8fb8-c042b4c21ee1','c6871ac3-eee2-5a4b-8486-ddc53c93d5be',3,'秩、线性相关与方程组 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('a7351894-5883-5fff-b850-b55d026f42df','例：x+y=1，2x+2y=2，秩为 1，有无穷多解 (1−t,t)。','例：x+y=1，2x+2y=2，秩为 1，有无穷多解 (1−t,t)。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('2fb27d7e-d99c-5a5a-803f-e253d6bb5edc','6450a55a-7d59-5632-b9ad-2a4e170af220','eigen');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc','特征值、特征向量与对角化','Av=λv，v≠0，则 λ 为特征值，v 为对应特征向量。
先解 det(A−λI)=0，再解 (A−λI)v=0。
特征值之和=tr(A)，乘积=det(A)，按代数重数计。
有 n 个线性无关特征向量 ⇔ n 阶矩阵可对角化：P⁻¹AP=Λ。
实对称矩阵可正交对角化，其不同特征值对应的特征向量正交。',3,'[{"label":"特征方程","tex":"\\\\det(A-\\\\lambda I)=0,\\\\qquad(A-\\\\lambda I)v=0","condition":"v≠0"},{"label":"对角化","tex":"A=P\\\\Lambda P^{-1},\\\\qquad A^n=P\\\\Lambda^nP^{-1}","condition":"具有 n 个线性无关特征向量"}]','[{"kind":"LINK","label":"MIT · Linear Algebra","url":"https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('1d628774-e6cc-5d6b-b544-76e3fbf230a1','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',1,'求 $\\operatorname{diag}(2,3)$ 的特征值。',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('1d628774-e6cc-5d6b-b544-76e3fbf230a1','$2,3$','对角矩阵的特征值是对角元。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('1ca446c6-7a80-5115-84e1-48d611c3cbbc','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',2,'若 $Av=2v$ 且 $v\\ne0$，求 $A^3v$。',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('1ca446c6-7a80-5115-84e1-48d611c3cbbc','$8v$','连续应用 Av=2v 三次。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ffd410db-9204-54cb-8afc-6c9ff0a7d9f3','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',3,'求 $\\begin{pmatrix}2&1\\\\1&2\\end{pmatrix}$ 的特征值。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ffd410db-9204-54cb-8afc-6c9ff0a7d9f3','$3,1$','特征多项式 (2−λ)²−1，根为 3 和 1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('182bc749-9443-530c-89c2-bfec5e2fa29b','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',4,'判断 $\\begin{pmatrix}1&1\\\\0&1\\end{pmatrix}$ 是否可对角化。',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('182bc749-9443-530c-89c2-bfec5e2fa29b','不可对角化。','唯一特征值 1 的代数重数为 2，而特征空间由 (1,0) 张成，维数只有 1。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('5e03f181-dfcf-55ad-af40-edfe8c65f0d9','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',5,'求 $A=\\begin{pmatrix}2&1\\\\1&2\\end{pmatrix}$ 的 $A^n$（$n\\ge0$）。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('5e03f181-dfcf-55ad-af40-edfe8c65f0d9','$A^n=\\frac12\\begin{pmatrix}3^n+1&3^n-1\\\\3^n-1&3^n+1\\end{pmatrix}$','选单位特征向量 $(1,1)/\\sqrt2$、$(1,-1)/\\sqrt2$ 组成正交矩阵 Q，得 $A=Q\\operatorname{diag}(3,1)Q^T$，幂运算后乘回。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('d943fadc-36b5-5044-a0ea-d3923929209c','619c8138-c56a-5ade-8fb8-c042b4c21ee1','2fb27d7e-d99c-5a5a-803f-e253d6bb5edc',3,'特征值、特征向量与对角化 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('d943fadc-36b5-5044-a0ea-d3923929209c','例：diag(2,3) 的特征值为 2、3，对应 (1,0)ᵀ、(0,1)ᵀ。','例：diag(2,3) 的特征值为 2、3，对应 (1,0)ᵀ、(0,1)ᵀ。');
INSERT INTO knowledge_module (id,course_id,stable_key) VALUES ('92a31e4d-87e8-51ae-b50b-e9a2e47b3620','6450a55a-7d59-5632-b9ad-2a4e170af220','quadratic');
INSERT INTO module_revision (release_id,module_id,title,content,difficulty,formulas,resources) VALUES ('619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620','二次型与正定性','实二次型 f(x)=xᵀAx，可取 A 为实对称矩阵。交叉项 2aᵢⱼxᵢxⱼ 对应两个对称位置。
正交变换可把实对称二次型化为特征值系数的平方和。
实对称 A 正定 ⇔ 所有特征值>0 ⇔ 所有顺序主子式>0。
易错：仅 det(A)>0 不足以判断正定。',4,'[{"label":"矩阵表达","tex":"f(x)=x^TAx,\\\\qquad A=A^T","condition":"交叉项系数 2aᵢⱼ"},{"label":"正定判定","tex":"A\\\\succ0\\\\iff\\\\lambda_i>0\\\\ (\\\\forall i)","condition":"实对称矩阵；等价于全部顺序主子式为正"}]','[{"kind":"LINK","label":"MIT · Linear Algebra","url":"https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/","fileId":null}]');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('c98b0106-0cb2-5cae-b906-2867addbd284','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',1,'$x^2+4xy+3y^2$ 的对称矩阵是什么？',0);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('c98b0106-0cb2-5cae-b906-2867addbd284','$\\begin{pmatrix}1&2\\\\2&3\\end{pmatrix}$','交叉项系数除以 2 填入两个对称位置。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('72c7d2c4-4e6a-5f78-b7dc-d1c58789acd9','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',2,'$x^2+y^2$ 是否正定？',1);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('72c7d2c4-4e6a-5f78-b7dc-d1c58789acd9','正定。','非零向量的平方和严格大于零。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('44d43a94-8e3f-5292-8aae-79caba16b27c','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',3,'判断 $x^2+2xy+3y^2$ 的正定性。',2);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('44d43a94-8e3f-5292-8aae-79caba16b27c','正定。','两个顺序主子式为 1、2，均为正。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('ec0fb33e-d31f-5306-945c-61ab3b48ef15','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',4,'使 $x^2+2axy+y^2$ 正定的 a 范围是什么？',3);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('ec0fb33e-d31f-5306-945c-61ab3b48ef15','$-1<a<1$','顺序主子式 1 和 1−a² 均需大于 0。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('631dd711-f2e3-5b70-a84a-a5f64631be6c','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',5,'讨论 $q=x^2+y^2+z^2+2a(xy+yz+zx)$ 的正定、半正定及不定区间。',4);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('631dd711-f2e3-5b70-a84a-a5f64631be6c','−1/2<a<1 正定；a=−1/2 或 1 半正定而非正定；其余不定。','矩阵为 $(1-a)I+aJ$。特征值 1+2a（沿 (1,1,1)）及二重 1−a（垂直子空间）。比较两者符号；区间外一正一负，因此不定。');
INSERT INTO knowledge_example (id,release_id,module_id,stars,question,sort_order) VALUES ('566cc9c5-68d7-5010-881e-f9d98971442f','619c8138-c56a-5ade-8fb8-c042b4c21ee1','92a31e4d-87e8-51ae-b50b-e9a2e47b3620',4,'二次型与正定性 · 教学示例',1000);
INSERT INTO example_solution (example_id,answer,solution) VALUES ('566cc9c5-68d7-5010-881e-f9d98971442f','例：x²+2xy+3y² 对应 [[1,1],[1,3]]，顺序主子式 1、2 均正，故正定。','例：x²+2xy+3y² 对应 [[1,1],[1,3]]，顺序主子式 1、2 均正，故正定。');
UPDATE content_release SET state='RETIRED' WHERE id='10a804de-4d52-55c6-a677-62783fa5de09';

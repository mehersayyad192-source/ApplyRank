/* One list of IT tracks shared by Job Corner, Skill Test and Recruiter desk.
   test:true  = a verified skill test exists for this track.
   uses:[..]  = no own test; the best rank of those tracks is used (Full Stack = best of Frontend / Backend). */
window.APPLYRANK_TRACKS = [
  { key: 'frontend',  name: 'Frontend Developer',  icon: 'palette',      color: '#ec4899', desc: 'React, JavaScript, CSS and pixel-perfect UI craft.', tags: ['React', 'JavaScript', 'CSS'], test: true },
  { key: 'backend',   name: 'Backend Developer',   icon: 'settings',     color: '#3b82f6', desc: 'Java, Spring Boot, databases and secure scalable APIs.', tags: ['Java', 'Spring Boot', 'SQL'], test: true },
  { key: 'fullstack', name: 'Full Stack Developer', icon: 'layers',      color: '#8b5cf6', desc: 'Frontend and backend together, end to end.', tags: ['React', 'Java', 'REST'], test: false, uses: ['frontend', 'backend'] },
  { key: 'data',      name: 'Data & SQL',          icon: 'database',     color: '#10b981', desc: 'SQL, analytics and data-driven decisions.', tags: ['SQL', 'Excel', 'Python'], test: true },
  { key: 'ml',        name: 'AI / ML Engineer',    icon: 'psychology',   color: '#f59e0b', desc: 'Python, machine learning and model deployment.', tags: ['Python', 'ML', 'TensorFlow'], test: true },
  { key: 'devops',    name: 'DevOps & Cloud',      icon: 'cloud',        color: '#06b6d4', desc: 'Docker, CI/CD, Linux and cloud platforms.', tags: ['Docker', 'AWS', 'CI/CD'], test: true },
  { key: 'mobile',    name: 'Mobile App Developer', icon: 'smartphone',  color: '#ef4444', desc: 'Android, Flutter or React Native apps.', tags: ['Kotlin', 'Flutter', 'Android'], test: true },
  { key: 'qa',        name: 'QA & Test Engineer',  icon: 'bug_report',   color: '#84cc16', desc: 'Manual and automation testing, bug hunting.', tags: ['Selenium', 'Testing', 'JUnit'], test: true },
  { key: 'cyber',     name: 'Cybersecurity',       icon: 'security',     color: '#f43f5e', desc: 'Network, application security and OWASP.', tags: ['OWASP', 'Networking', 'Linux'], test: true },
  { key: 'uiux',      name: 'UI / UX Designer',    icon: 'draw',         color: '#d946ef', desc: 'Figma, wireframes and user research.', tags: ['Figma', 'Wireframes', 'UX'], test: true }
];

import {HomeView} from '@/components/HomeView';
import {homeMetadata} from '@/lib/seo';

export const revalidate = 3600;
export const metadata = homeMetadata('en');
export default function Home() {
  return <HomeView lang="en" />;
}
